import argparse
import io
import json
from pathlib import Path
import runpy
import tempfile
import unittest
from unittest.mock import patch
from urllib.parse import parse_qs,urlparse

module=runpy.run_path(str(Path(__file__).resolve().parents[1]/'import_public_places.py'))

def row(ident='1'):
    return dict(contentid=ident,title='실제 원본 장소',addr1='서울특별시 성동구',mapy='37.5446',mapx='127.0557',lDongRegnCd='11',contenttypeid='39',lclsSystm3='FD050200')

class PublicImportTest(unittest.TestCase):
    def test_current_region_parameter_and_all_pages(self):
        calls=[]
        def response(url,timeout):
            query=parse_qs(urlparse(url).query);calls.append(query)
            self.assertEqual(query['lDongRegnCd'],['11']);self.assertNotIn('areaCode',query)
            item=row(str(len(calls)))
            return io.BytesIO(json.dumps({'response':{'header':{'resultCode':'0000'},'body':{'totalCount':2,'items':{'item':[item]}}}}).encode())
        with tempfile.TemporaryDirectory() as folder:
            path=Path(folder)/'catalog.json'
            with patch.dict(module['os'].environ,{'DATA_GO_KR_SERVICE_KEY':'test-key'}),patch.object(module['urllib'].request,'urlopen',side_effect=response):
                module['collect'](argparse.Namespace(region_code='11',file=str(path)))
            data=json.loads(path.read_text());self.assertEqual(len(calls),2);self.assertEqual(len(data['accepted']),2)
            self.assertEqual(data['accepted'][0]['category'],'CAFE');self.assertNotIn('test-key',path.read_text())

    def test_invalid_or_other_region_coordinates_are_not_published(self):
        for change in [dict(mapx='0',mapy='0'),dict(lDongRegnCd='26'),dict(addr1='부산광역시'),dict(contentid='bad-id')]:
            with self.subTest(change=change):
                value=row();value.update(change);normalized,reason=module['normalize'](value,'11');self.assertIsNone(normalized);self.assertTrue(reason)

    def test_restaurant_is_not_reclassified_by_its_name(self):
        value=row();value.update(title='카페라는 이름이 있는 식당',lclsSystm3='FD020300')
        self.assertEqual(module['normalize'](value,'11')[0]['category'],'FOOD')

if __name__=='__main__':unittest.main()
