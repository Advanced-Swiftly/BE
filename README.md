# 🗣️ Swiftly BE
## 🚀 Git Flow
- `main`
    - 프로젝트 최종 merge
    - 기본 프로젝트 세팅, 배포 가능한 브랜치, 항상 배포 가능한 상태를 유지
- `develop`
    - 프로젝트 완성 전까지 완성한 기능들을 계속해서 merge
    - 배포 가능한 브랜치, 항상 배포 가능한 상태를 유지
- `{type}/{issue number}`
    - 개발 브랜치
    - 예: `feat/#5`, `fix/#11`

> 작업 단위로 이슈 생성 → 브랜치 생성 → 생성한 브랜치에서 작업 후 끝나면 dev 브랜치로 PR 남기기
>
> 모든 작업 시작 전 생성한 브랜치에서 dev 브랜치 pull을 받은 후 작업

<!-- <img width="838" height="718" alt="Image" src="https://github.com/user-attachments/assets/530e9719-468e-457a-981a-e5fa46af82ff" /> -->

&nbsp;
## 💡 PR Rules
- Assignee에는 본인을 지정해 주세요.
- Reviewers에는 본인을 제외한 백엔드 팀원을 지정한 후, 카카오톡으로 공유해 주세요.
- 이후, 팀원이 PR을 확인하고 승인해서 머지해 주세요.
  (해당 브랜치는 머지 후 자동 삭제되며, 복구도 가능합니다.)

&nbsp;
## 💻 Commit Message Convention
| **Type** | **Description** |
| --- | --- |
| **Feat** | 새로운 기능 추가 |
| **Fix** | 버그 수정 |
| **Docs** | 문서 수정 |
| **Style** | 코드 formatting, 세미콜론 누락, 코드 자체의 변경이 없는 경우 |
| **Refactor** | 코드 리팩토링 |
| **Test** | 테스트 코드, 리팩토링 테스트 코드 추가 |
| **Chore** | 패키지 매니저 수정, 그 외 기타 수정 (예: .gitignore) |
| **Design** | CSS 등 사용자 UI 디자인 변경 |
| **Comment** | 필요한 주석 추가 및 변경 |
| **Rename** | 파일 또는 폴더 명을 수정하거나 옮기는 작업만인 경우 |
| **Remove** | 파일을 삭제하는 작업만 수행한 경우 |
| **Init** | 프로젝트 초기 세팅 |
| **Merge** | 브랜치 merge |
| **!BREAKING CHANGE** | 커다란 API 변경의 경우 |
| **!HOTFIX** | 급하게 치명적인 버그를 고쳐야 하는 경우 |
> Type: commit title
>
> ex. `Feat: 로그인 기능 추가`