package com.eaiagent.service;

/**
 * eai_agent <-> eai_server TCP 소켓 프로토콜 상수. eaiserver 프로젝트의 동일 클래스와 반드시
 * 값이 일치해야 한다(레포가 분리돼 있어 코드 공유는 안 되고, 값만 맞춰서 관리한다).
 * 자세한 프로토콜 설명은 eaiserver 쪽 EaiProtocol.java 주석 참고.
 */
public final class EaiProtocol {
    public static final byte OP_SEND = 0x01;
    public static final byte OP_FETCH_RCV = 0x02;

    private EaiProtocol() {
    }
}
