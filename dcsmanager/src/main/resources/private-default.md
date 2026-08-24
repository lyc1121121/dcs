# PRIVATE — dcs_client_ne 작업 이력

`dcs_client_ne`(Visual Studio 2019 MFC 클라이언트를 리눅스로 포팅하는 작업) 관련
모든 작업 이력을 여기에 기록합니다. 앞으로 `dcs_client_ne` 관련 작업이 있을 때마다
이 탭에 이어서 기록됩니다.

## 진행 경과 (todo.txt 128~131단계)

- **128단계**: `/working/dcs_client_ne/KH_testVC_VS2019_copy`(Visual Studio 2019, MFC)
  안의 `Slim_Main_Download()`/`Slim_Main_Upload()` 두 함수를 리눅스 C로 포팅해달라는
  요청. 처음엔 `testVCDlg.cpp`에서 두 함수를 찾지 못해 막힘 — 실제로는 다른 파일에
  있었음.
- **129단계**: 사용자가 호출 관계를 알려줌 — `testVCDlg.cpp`의 `_Thread_Process()`가
  `SocketProcess.cpp`의 `CSocketProcess::MainFunc()`를 거쳐 두 함수를 호출. 여기서
  DES 인증에 Windows 전용 `Des.dll`(소스 없음)을 쓴다는 것도 확인됨 — 사용자가
  `/working/dcs_client_ne/DES/src/des.c`(DES 알고리즘 순수 C 소스)를 제공해줘서
  해결.
- **130단계**: 포팅한 프로그램이 실제로 어느 서버/파일과 통신하는지 검토 —
  `Slim_Main_Download()`가 `dcs1300100090`이 아니라 `dcs1300100021`로 접속하고
  있다는 포트 불일치를 발견(호스트 포트 20000이 실제로는 dcs1300100021에 매핑되어
  있었음). 사용자가 "그냥둬"라고 해서 수정하지 않고 현재 상태 유지.
- **131단계**: 지금 이 "PRIVATE" 탭 신설 요청 — dcs_client_ne 관련 전체 이력을
  기록하고, admin/admin 비밀번호로만 볼 수 있게 함.

## 포팅 결과 요약

- 만든 파일: `main.c`, `shareSrc/`(dcs_types.h, client_socket.{h,c}, des.{h,c},
  dcs_auth.{h,c}, dcs_packet.{h,c}, dcs_client.{h,c}), `Makefile`, `history.txt`
- 실제 서버(100.125.13.91)에 대해 다운로드/업로드 둘 다 end-to-end 테스트 성공
  (다운로드 1,015,882바이트 수신, 업로드 9,000바이트를 3청크로 송신)
- GitHub `https://github.com/lyc1121121/private_git`에 원본 참고 소스
  (`KH_testVC_VS2019_copy`) + 포팅 결과물(`main.c`/`shareSrc`/`Makefile`/
  `history.txt`) 전부 업로드 완료

## history.txt 전문

아래는 `/working/dcs_client_ne/history.txt` 파일의 내용입니다(포팅 과정에서의
구체적인 결정/수정 사항 기록).

```
dcs_client_ne 포팅 작업이력
=================================

배경 (todo.txt 128/129단계)
---------------------------
- /working/dcs_client_ne/KH_testVC_VS2019_copy 에 Visual Studio 2019(MFC)로 작성된
  testVCDlg.cpp 기반 클라이언트가 있고, 그 안의 두 핵심 함수
    - Slim_Main_Download()  : SlimDCS 운영정보(버전) 다운로드 테스트
    - Slim_Main_Upload()    : SlimDCS 거래파일(JAQT) 업로드 테스트
  를 CentOS 리눅스에서 동작하도록 포팅하는 것이 목표.
- 128단계 검토 중, 이 두 함수는 testVCDlg.cpp가 아니라 SocketProcess.cpp/.h
  안에 있다는 걸 확인함(129단계에서 사용자가 알려준 호출관계:
  testVCDlg.cpp 의 _Thread_Process() -> CSocketProcess::MainFunc()
  -> MainFunc_Slim_200601() -> Slim_Main_Upload()/Slim_Main_Download()).
- 포팅 도중 Process_SendAuthServer() 가 인증 단계에서 Windows 전용 Des.dll
  (소스 없음, .lib/.dll 바이너리만 존재)을 쓴다는 걸 발견해 한 차례 막혔는데,
  사용자가 /working/dcs_client_ne/DES/src/des.c 를 제공해줘서 해결함 - 이 파일이
  바로 그 DES 알고리즘의 순수 C 소스(TransEncrypt/TransDecrypt 함수 +
  g_desInitialKey/g_desFixedKey 키값)였음.

무엇을 만들었나
---------------
- /working/dcs_client_ne/main.c                    - 새 진입점
- /working/dcs_client_ne/Makefile                  - 빌드 스크립트
- /working/dcs_client_ne/shareSrc/dcs_types.h       - 프로토콜 구조체/상수
- /working/dcs_client_ne/shareSrc/client_socket.{h,c} - CClientSocket -> POSIX 소켓
- /working/dcs_client_ne/shareSrc/des.{h,c}         - 사용자 제공 DES 소스 복사(전역변수만 개명)
- /working/dcs_client_ne/shareSrc/dcs_auth.{h,c}    - 인증/키생성/BCD/BCC/파일유틸
- /working/dcs_client_ne/shareSrc/dcs_packet.{h,c}  - 패킷 송수신 계층
- /working/dcs_client_ne/shareSrc/dcs_client.{h,c}  - Slim_Main_Download/Upload 및 그 하위 로직
- /working/dcs_client_ne/data/                      - 업로드 원본/다운로드 저장용 테스트 폴더

실행 결과: 실제 서버(100.125.13.91, 다운로드는 20000번, 업로드는 30001번 포트)에
접속해서 둘 다 성공 확인함.
  - Slim_Main_Download(): 인증 통과 후 운영정보 파일 1,015,882바이트 수신
    (data/TEMP_JAPX0.20171208210016.20171208220034 로 저장, gzip 포맷 확인됨)
  - Slim_Main_Upload(): 테스트 파일(JAQT.0.130010002.196510005.20181203095618.C,
    9000바이트 더미 데이터)을 3000바이트씩 3개 청크로 전송, EOF 응답까지 정상 수신

빌드/실행 방법
--------------
  cd /working/dcs_client_ne
  make                 # dcs_client 실행파일 생성
  ./dcs_client          # 다운로드 + 업로드 둘 다 실행 (기본값)
  ./dcs_client download # 다운로드만
  ./dcs_client upload   # 업로드만

원본과 다르게 결정/수정한 부분 (전부 원본 코드가 SlimDCS 테스트 경로를 위해
지역적으로 켜둔 매크로 조합 - SocketProcess.cpp 상단의
#define _USE_SLIM 1 / #define TESTMODE_LYC 1, _USE_CONT_DOWN 은 비활성 -
을 확인한 뒤 그 경로만 그대로 옮기는 것을 전제로 함)
---------------------------------------------------------------------------
1. long -> int32_t (중요한 정확성 수정)
   프로토콜 구조체(_PackHeaderInfo, _FileTransHeader 등)는 #pragma pack(1) +
   long 필드로 정의돼 있는데, long 은 Windows(32비트)에서 4바이트지만 리눅스
   x86_64에서는 8바이트다. 그대로 옮기면 패킷 크기/오프셋이 실제 서버와
   어긋나서 통신이 깨진다. 그래서 프로토콜 구조체의 long/DWORD 필드는 전부
   int32_t/uint32_t로 명시했다(구조체의 실제 바이트 레이아웃은 원본과 동일).

2. Des.dll -> des.c 직접 링크
   Des.h가 선언하던 T_initial_key/T_fixed_key/T_encrypt/T_decrypt 를, 사용자가
   제공한 des.c(TransEncrypt/TransDecrypt + 하드코딩된 키)를 감싸는 얇은
   wrapper로 shareSrc/dcs_auth.c 에 구현했다. des.c 자체의 전역변수
   InitialKey/gachFixedKey 는 CommonDef 쪽의 동명 전역(InitialKey/FixedKey)과
   이름이 겹쳐서 g_desInitialKey/g_desFixedKey로 바꿔서 복사해왔다
   (shareSrc/des.c, shareSrc/des.h).

3. CreateKey2(): 서버 난수 덮어쓰기 로직 제외
   원본은 "#if !defined(_TEST_VC) GenerateRandomNo(svr_rnd_no); #endif" 로,
   _TEST_VC 가 정의 안 된 빌드에서는 서버가 보낸 난수(svr_rnd_no)를 클라이언트가
   임의로 재생성한 값으로 덮어썼다. 그런데 이 값은 서버가 만들어 보낸 것이라,
   덮어쓰면 서버와 클라이언트가 서로 다른 값으로 키를 유도하게 되어 실제
   프로토콜과 어긋난다(다운로드 테스트 성공으로 이 판단이 맞았음을 확인함).
   그래서 이 덮어쓰기는 포팅하지 않고, 항상 서버가 보낸 svr_rnd_no를 그대로 쓰게 했다.

4. Windows 경로 -> 리눅스 경로
   원본의 "C:\\1\\"(업로드 원본 폴더), "C:\\1\\TEMP_%s"(다운로드 저장 경로)를
   /working/dcs_client_ne/data/ 로 바꿨다. 이건 OS 차이상 불가피한 변경이라
   그대로 대체했고, 다른 곳처럼 "원본 그대로 보존"하지 않았다.

5. 다운로드 파일명 끝 공백 제거 (신규 보정, 원본엔 없음)
   서버가 FileTransHeader.FileName[50] 필드를 공백으로 패딩해서 보내는데,
   윈도우 탐색기/API는 파일명 끝 공백을 보통 무시하지만 리눅스 파일시스템은
   그 공백을 그대로 파일명의 일부로 만들어버린다(실제로 포팅 직후 첫 테스트에서
   "TEMP_JAPX0...220034               " 처럼 끝에 공백이 붙은 파일이 생기는 걸
   확인함). 그래서 저장 파일명을 만들 때 끝쪽 공백/제어문자를 잘라내는 보정을
   추가했다.

6. Process_SendAuthServer()의 'AC' 메시지 검증
   원본은 _USE_SLIM 분기에서 Process_Slim_New_MyReceivePacket()으로 헤더를
   받아놓고, 그 직후 정작 비교는 그 결과가 아니라 한 번도 채워진 적 없는
   RxBuffer를 다시 읽어서 했다(초기화되지 않은/오래된 메모리를 비교하는 셈).
   다행히 Process_SendAuthServer()의 반환값은 두 호출부(Slim_Main_Upload,
   MainFunc_GetOperInfo_Continue_200601) 모두 로그만 남기고 실제 흐름을
   막는 데 쓰지 않아서 전체 동작에는 영향이 없었지만, C에서 초기화 안 된
   스택 메모리를 비교하는 건 안전하지 않아서, 실제로 수신된 헤더를 비교하도록
   고쳤다(호출측 동작은 원본과 동일하게 "실패해도 계속 진행"으로 유지).

포팅 범위 밖(원본에서 이번에 옮기지 않은 부분)
-----------------------------------------------
- Daejon/Saejong/TOWN/PoHang 등 다른 MainFunc_* 테스트 경로들 (28단계 요청은
  Slim_Main_Download/Upload 두 개로 범위가 명확했음)
- 다운로드의 "구버전(1024바이트 패킷)" 분기, Process_New_MyReceivePacket -
  실제 호출 경로(bNewFW=true)가 "신버전(4096바이트 스트림)" 분기만 타므로
  이번 포팅에서는 4096 분기만 옮겼다.
- _USE_CONT_DOWN(이어받기 테스트) 경로 - 원본에서도 비활성 상태였음
- MFC 다이얼로그/버튼 UI 자체 - 이번 요청은 콘솔 프로그램(main.c)이라 UI는
  대상이 아니었음
```

## 알려진 이슈 (수정 보류)

- `Slim_Main_Download()`가 `SERVER_PORT_NO`(20000) 상수를 그대로 써서 실제로는
  `dcs1300100090`이 아니라 `dcs1300100021`로 접속한다(130단계에서 발견). 업로드는
  30001번 포트를 써서 정확히 `dcs1300100090`으로 간다. 사용자가 "그냥둬"라고 해서
  현재 상태를 유지 중 — 필요해지면 `nServerPortNo` 파라미터를 실제 접속에
  사용하도록 고치면 된다(포트 20001).
