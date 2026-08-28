package com.dcsmanager.domain;

/**
 * DCS_ID(10자리 숫자)로부터 DCS_DOT_ID를 생성한다.
 * 예: 1300100021 -> 1.30.010002.1 (1자리.2자리.6자리.1자리)
 * 이 분할 규칙은 todo.txt 예시 1건만으로 추정한 것이므로, 다른 DCS_ID 사례로 검증이 필요하다.
 */
public final class DcsIdUtils {

    private static final int[] SEGMENT_LENGTHS = {1, 2, 6, 1};

    private DcsIdUtils() {
    }

    public static String toDotId(String dcsId) {
        if (dcsId == null || !dcsId.matches("\\d{10}")) {
            throw new IllegalArgumentException("TMS_ID는 10자리 숫자여야 합니다: " + dcsId);
        }
        StringBuilder sb = new StringBuilder();
        int pos = 0;
        for (int i = 0; i < SEGMENT_LENGTHS.length; i++) {
            int len = SEGMENT_LENGTHS[i];
            if (i > 0) {
                sb.append('.');
            }
            sb.append(dcsId, pos, pos + len);
            pos += len;
        }
        return sb.toString();
    }
}
