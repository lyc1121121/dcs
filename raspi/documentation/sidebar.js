// 321단계 추가: 실제 raspberrypi.com처럼, 사이드바의 어떤 항목이든 클릭하면
// 페이지 이동 없이 그 자리에서 하위 섹션 목록이 펼쳐지는 아코디언 동작.
// DOC_SECTIONS: 14개 문서 페이지 각각의 h2 섹션(id, 제목) 목록.
var DOC_SECTIONS = {"getting-started": [["sec-1", "개요"], ["sec-2", "하드웨어 준비물"], ["sec-3", "네트워킹"], ["sec-4", "Raspberry Pi 설정하기"], ["sec-5", "부팅 미디어에 OS 설치하기"]], "os": [["sec-1", "개요"], ["sec-2", "설치 방법 &amp; 에디션"], ["sec-3", "소프트웨어/펌웨어 업데이트"], ["sec-4", "패키지 관리"], ["sec-5", "주요 버전 업그레이드"], ["sec-6", "오디오/비디오 재생"], ["sec-7", "커맨드라인 유틸리티"], ["sec-8", "접근성 기능"], ["sec-9", "파이썬 개발"], ["sec-10", "Python으로 GPIO 제어"]], "configuration": [["sec-1", "설정 방법 3가지"], ["sec-2", "오디오/영상 출력"], ["sec-3", "네트워킹"], ["sec-4", "무선 연결"], ["sec-5", "시스템 및 지역화"], ["sec-6", "사용자 접근 관리"], ["sec-7", "하드웨어 통신"], ["sec-8", "성능"], ["sec-9", "LED 동작"], ["sec-10", "부팅 동작"], ["sec-11", "펌웨어"], ["sec-12", "외부 저장장치"]], "config-txt": [["sec-1", "config.txt란?"], ["sec-2", "파일 형식"], ["sec-3", "autoboot.txt"], ["sec-4", "주요 디스플레이 옵션"], ["sec-5", "온보드 아날로그 오디오"], ["sec-6", "HDMI 오디오"], ["sec-7", "주요 하드웨어 설정 옵션"], ["sec-8", "부팅 설정"], ["sec-9", "GPIO 제어"]], "linux-kernel": [["sec-1", "소개"], ["sec-2", "업데이트"], ["sec-3", "커널 빌드하기 - 개요"], ["sec-4", "커널 설정 구성"], ["sec-5", "커널 패치하기"], ["sec-6", "커널 헤더"], ["sec-7", "기여하기"]], "remote-access": [["sec-1", "SSH"], ["sec-2", "VNC"], ["sec-3", "Raspberry Pi Connect"], ["sec-4", "Raspberry Pi의 IP 주소 찾기"], ["sec-5", "SCP로 파일 공유하기"], ["sec-6", "rsync로 폴더 동기화하기"], ["sec-7", "네트워크 파일시스템(NFS)"], ["sec-8", "Samba(SMB/CIFS)"], ["sec-9", "Apache 웹서버 설치"], ["sec-10", "네트워크 부팅"]], "camera-software": [["sec-1", "개요"], ["sec-2", "libcamera 기반"], ["sec-3", "rpicam-apps 도구 모음"], ["sec-4", "기본 사용 예시"], ["sec-5", "주요 설정"], ["sec-6", "다중 카메라 지원"]], "ai-software": [["sec-1", "개요"], ["sec-2", "하드웨어 옵션"], ["sec-3", "하드웨어 준비물"], ["sec-4", "소프트웨어 설정 단계"], ["sec-5", "비전 AI 모델"], ["sec-6", "LLM (AI HAT+ 2 전용)"], ["sec-7", "선택 사항: 웹 인터페이스(Open WebUI)"], ["sec-8", "참고 자료"]], "hardware": [["sec-1", "소개"], ["sec-2", "대표 싱글보드 컴퓨터 시리즈"], ["sec-3", "키보드 컴퓨터 시리즈"], ["sec-4", "Zero 시리즈"], ["sec-5", "Compute Module 시리즈"], ["sec-6", "Pico 마이크로컨트롤러 보드"], ["sec-7", "규제 준수"]], "keyboard-computers": [["sec-1", "개요"], ["sec-2", "주요 사양 비교"], ["sec-3", "키보드 컴퓨터 키트 구성품"], ["sec-4", "Pi 500+만의 특징"], ["sec-5", "시작하기"], ["sec-6", "고급 설정"]], "compute-module": [["sec-1", "Compute Module이란?"], ["sec-2", "IO 보드와의 관계"], ["sec-3", "현재 모델"], ["sec-4", "eMMC에 이미지 플래시하기"], ["sec-5", "EEPROM 부트로더 설정"]], "processors": [["sec-1", "SoC 비교표"], ["sec-2", "BCM2835"], ["sec-3", "BCM2836"], ["sec-4", "BCM2837"], ["sec-5", "BCM2837B0"], ["sec-6", "BCM2711"], ["sec-7", "BCM2712"], ["sec-8", "RP3A0"]], "io-controllers": [["sec-1", "RP1이란?"], ["sec-2", "물리적 사양"], ["sec-3", "제공하는 주변장치 인터페이스"], ["sec-4", "내부 구성요소"], ["sec-5", "멀티미디어 기능"], ["sec-6", "RP1이라는 이름의 유래"]], "software-sources": [["sec-1", "개요"], ["sec-2", "Raspberry Pi OS에서 소프트웨어 소스 찾기"], ["sec-3", "GitHub 조직"], ["sec-4", "주요 저장소"]]};

document.addEventListener('DOMContentLoaded', function () {
    var sidebar = document.querySelector('.doc-sidebar');
    if (!sidebar) { return; }
    var currentSlug = sidebar.getAttribute('data-current-slug');

    var tris = sidebar.querySelectorAll('.tri');
    for (var i = 0; i < tris.length; i++) {
        (function (tri) {
            tri.addEventListener('click', function (e) {
                e.preventDefault();
                e.stopPropagation();
                var li = tri.closest('li');
                var slug = tri.getAttribute('data-slug');
                var subnav = li.querySelector('.doc-subnav');
                var expanded = tri.getAttribute('aria-expanded') === 'true';

                if (!subnav.dataset.populated) {
                    var sections = DOC_SECTIONS[slug] || [];
                    var prefix = (slug === currentSlug) ? '' : (slug + '.html');
                    subnav.innerHTML = sections.map(function (s) {
                        return '<li><a href="' + prefix + '#' + s[0] + '">' + s[1] + '</a></li>';
                    }).join('');
                    subnav.dataset.populated = '1';
                }

                if (expanded) {
                    subnav.hidden = true;
                    tri.textContent = '\u25b6';
                    tri.setAttribute('aria-expanded', 'false');
                } else {
                    subnav.hidden = false;
                    tri.textContent = '\u25bc';
                    tri.setAttribute('aria-expanded', 'true');
                }
            });
        })(tris[i]);
    }
});
