package com.eaiserver.service;

/**
 * eai_agent <-> eai_server TCP 소켓 프로토콜 상수. eaiagent 프로젝트의 동일 클래스와 반드시
 * 값이 일치해야 한다(레포가 분리돼 있어 코드 공유는 안 되고, 값만 맞춰서 관리한다).
 *
 * 연결 1개 = 요청 1건. 맨 처음 opType 1바이트로 무슨 요청인지 구분한다.
 *
 * OP_SEND (에이전트가 파일을 서버로 올림, SND 방향)
 *   요청: writeByte(OP_SEND), writeUTF(dcsId), writeUTF(fileName), writeLong(fileLength), write(fileBytes)
 *   응답: writeByte(status 1|0), writeUTF(message)
 *
 * OP_FETCH_RCV (에이전트가 자기 앞으로 온 파일이 있는지 물어봄, RCV 방향)
 *   요청: writeByte(OP_FETCH_RCV), writeUTF(dcsId)
 *   응답: writeByte(hasFile 1|0)
 *         hasFile==1 이면 이어서: writeUTF(fileName), writeLong(fileLength), write(fileBytes)
 *              그 다음 에이전트가 writeByte(ackStatus 1|0) 로 수신 성공 여부를 알려줘야 하고,
 *              서버는 ackStatus==1 일 때만 그 파일을 outbox 에서 지운다(1==0 이면 다음 폴링에 재시도).
 */
public final class EaiProtocol {
    public static final byte OP_SEND = 0x01;
    public static final byte OP_FETCH_RCV = 0x02;

    private EaiProtocol() {
    }
}
