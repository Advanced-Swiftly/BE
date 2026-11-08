// Node.js 22 이상. 기본 실행은 DB에 쓰지 않고 집계만 출력합니다.
import { spawnSync } from 'node:child_process';
import { pathToFileURL } from 'node:url';

const endpoint = 'https://apis.data.go.kr/B551011/KorPetTourService2/areaBasedList2';
const categories = {
  12: 'ATTRACTION', 14: 'CULTURE', 15: 'EVENT', 28: 'LEISURE',
  32: 'LODGING', 38: 'SHOPPING', 39: 'FOOD',
};

export function normalize(item) {
  const id = String(item.contentid ?? '').trim();
  const name = String(item.title ?? '').trim();
  const lat = String(item.mapy ?? '').trim();
  const lon = String(item.mapx ?? '').trim();
  if (!/^\d{1,80}$/.test(id) || !name || !lat || !lon ||
      !Number.isFinite(Number(lat)) || !Number.isFinite(Number(lon)) ||
      Math.abs(Number(lat)) > 90 || Math.abs(Number(lon)) > 180) return null;
  return {
    external_id: id,
    name: Array.from(name).slice(0, 150).join(''),
    address: Array.from([item.addr1, item.addr2].filter(Boolean).join(' ').trim()).slice(0, 255).join('') || null,
    category: categories[item.contenttypeid] ?? null,
    latitude: Number(lat),
    longitude: Number(lon),
  };
}

export async function collect(key, request = fetch) {
  const entries = new Map();
  let skipped = 0;
  let seen = 0;
  for (let page = 1; page <= 200; page++) {
    const url = new URL(endpoint);
    url.search = new URLSearchParams({
      serviceKey: key, MobileOS: 'ETC', MobileApp: 'PawFit',
      _type: 'json', arrange: 'A', pageNo: String(page), numOfRows: '100',
    });
    const signal = AbortSignal.timeout(15000);
    let data;
    try {
      const response = await request(url, { signal, redirect: 'error' });
      if (!response.ok) throw new Error('관광공사 HTTP 응답 실패');
      data = (await response.json()).response;
    } catch (error) {
      if (signal.aborted) throw new Error('관광공사 요청 시간 초과');
      throw error;
    }
    if (data?.header?.resultCode !== '0000' && data?.header?.resultCode !== '00') {
      throw new Error('관광공사 API 결과 코드 오류');
    }
    const body = data.body;
    const raw = body?.items?.item;
    const items = Array.isArray(raw) ? raw : raw && typeof raw === 'object' ? [raw] : [];
    const total = /^\d+$/.test(String(body?.totalCount)) ? Number(body.totalCount) : NaN;
    if (!Number.isInteger(total) || total < 0 || (items.length === 0 && seen < total)) {
      throw new Error('관광공사 페이지 응답 형식 오류');
    }
    for (const item of items) {
      const row = normalize(item);
      if (row) entries.set(row.external_id, row);
      else skipped++;
    }
    seen += items.length;
    if (seen >= total) return { rows: [...entries.values()], skipped };
  }
  throw new Error('페이지 한도를 초과했습니다');
}

export function importSql(rows) {
  const payload = Buffer.from(JSON.stringify(rows), 'utf8').toString('base64');
  return `
    INSERT INTO public_facility
        (source_code, external_id, name, address, category, latitude, longitude, visible)
    SELECT 'KTO_PET_TOUR', external_id, name, address, category, latitude, longitude, FALSE
    FROM jsonb_to_recordset(convert_from(decode('${payload}', 'base64'), 'UTF8')::jsonb)
      AS r(external_id text, name text, address text, category text, latitude numeric, longitude numeric)
    ON CONFLICT (source_code, external_id) DO NOTHING;
  `;
}

async function main() {
  const args = process.argv.slice(2);
  if (args.length > 1 || (args.length === 1 && args[0] !== '--apply')) {
    throw new Error('사용법: node --env-file=.env scripts/import-pet-tour.mjs [--apply]');
  }
  const key = process.env.DATA_GO_KR_SERVICE_KEY;
  if (!key) throw new Error('DATA_GO_KR_SERVICE_KEY가 필요합니다');
  const { rows, skipped } = await collect(key);
  console.log(`유효한 고유 시설 ${rows.length}건, 필수 정보 누락 ${skipped}건`);
  if (args[0] !== '--apply' || rows.length === 0) return;
  if (!process.env.PGDATABASE) throw new Error('PGDATABASE로 대상 DB를 명시하세요');
  const result = spawnSync('psql', ['-X', '-v', 'ON_ERROR_STOP=1'], {
    input: importSql(rows), encoding: 'utf8',
  });
  if (result.status !== 0) throw new Error('DB 반영 실패. 연결 설정과 V10 적용 여부를 확인하세요');
  console.log(result.stdout.trim());
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  main().catch(error => {
    // 외부 요청 오류에 서비스 키가 포함된 URL이 노출되지 않도록 원본 예외를 출력하지 않습니다.
    const safe = ['사용법:', 'DATA_GO_KR_', '관광공사 ', '페이지 ', 'PGDATABASE', 'DB 반영 실패'];
    console.error(safe.some(prefix => error.message.startsWith(prefix))
      ? error.message : '관광공사 응답을 읽거나 연결하지 못했습니다');
    process.exitCode = 1;
  });
}
