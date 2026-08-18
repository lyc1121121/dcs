package com.eaiserver.service;

import org.springframework.stereotype.Service;

import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 114/115단계: DCSManager RCV 테스트에서 업로드한 J 파일을,
 * - 파일명 안의 날짜/시각을 오늘 기준으로 치환하고
 * - 짝이 되는 I 파일(XML)을 자동 생성한다.
 *
 * 매핑 규칙은 /data001/eai_outbox/1300100090/sample 의 실제 샘플(J0120.../I012.0_SSC.WAS_...)을
 * 분석해서 확정했다:
 *   - J 파일명: J{4자리코드}.{yyyyMMddHHmmss}.{yyyyMMdd}000000
 *   - I 파일명: I{코드 앞3자리}.{코드 4번째자리}_SSC.WAS_{rcv_sysid}_{yyyyMMdd}_{5자리 일련번호}
 *   - rcv_sysid: DCS_ID 마지막 한 글자(보통 0)를 뺀 9자리를 1+2+6자리로 나눠 점으로 연결 + 끝에 ".^"
 */
@Service
public class JToIFileMapper {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final Pattern J_FILENAME = Pattern.compile("^J(\\d{4})\\.[^.]+\\.[^.]+$");
    public static final Charset EUC_KR = Charset.forName("EUC-KR");

    private final RcvSequenceStore sequenceStore;

    public JToIFileMapper(RcvSequenceStore sequenceStore) {
        this.sequenceStore = sequenceStore;
    }

    public static boolean isJFileName(String fileName) {
        return fileName != null && J_FILENAME.matcher(fileName).matches();
    }

    public static class Mapped {
        public final String jFileName;
        public final String iFileName;
        public final byte[] iFileContent;

        public Mapped(String jFileName, String iFileName, byte[] iFileContent) {
            this.jFileName = jFileName;
            this.iFileName = iFileName;
            this.iFileContent = iFileContent;
        }
    }

    /**
     * 업로드된 원본 J 파일명(날짜 치환 전)과 dcsId 를 받아서, 오늘 날짜로 치환된 J 파일명과
     * 그 짝이 되는 I 파일(파일명 + XML 내용)을 만들어 반환한다.
     */
    public Mapped map(String originalJFileName, String dcsId) {
        Matcher m = J_FILENAME.matcher(originalJFileName);
        if (!m.matches()) {
            throw new IllegalArgumentException("J 파일명 형식이 아닙니다: " + originalJFileName);
        }
        String code4 = m.group(1);

        LocalDateTime now = LocalDateTime.now(KST);
        String nowTs = now.format(TS_FMT);
        String today = LocalDate.now(KST).format(DATE_FMT);

        String renamedJFileName = "J" + code4 + "." + nowTs + "." + today + "000000";

        String rcvSysid = toRcvSysid(dcsId);
        int seq = sequenceStore.nextSequence(dcsId);
        String iFileName = "I" + code4.substring(0, 3) + "." + code4.substring(3, 4)
                + "_SSC.WAS_" + rcvSysid + "_" + today + "_" + String.format("%05d", seq);

        String xml = buildXml(iFileName, nowTs, rcvSysid, renamedJFileName);
        return new Mapped(renamedJFileName, iFileName, xml.getBytes(EUC_KR));
    }

    /** DCS_ID 마지막 글자를 뺀 9자리를 1+2+6자리로 나눠 "1.30.010009.^" 형태로 만든다. */
    private String toRcvSysid(String dcsId) {
        String shortDcsId = dcsId.length() > 1 ? dcsId.substring(0, dcsId.length() - 1) : dcsId;
        if (shortDcsId.length() != 9) {
            throw new IllegalArgumentException("DCS_ID 형식이 예상과 다릅니다(10자리 숫자여야 함): " + dcsId);
        }
        return shortDcsId.substring(0, 1) + "." + shortDcsId.substring(1, 3) + "." + shortDcsId.substring(3, 9) + ".^";
    }

    private String buildXml(String iFileName, String makeTime, String rcvSysid, String jFileName) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"euc-kr\"?>\n");
        sb.append("<xml_file>\n");
        sb.append("<xml_format_info>\n");
        sb.append("<version>1.1</version>\n");
        sb.append("</xml_format_info>\n");
        sb.append("<file_info>\n");
        sb.append("<filename>").append(iFileName).append("</filename>\n");
        sb.append("<make_time>").append(makeTime).append("</make_time>\n");
        sb.append("<snd_time></snd_time>\n");
        sb.append("<snd_sysid>SSC.WAS</snd_sysid>\n");
        sb.append("<rcv_sysid>").append(rcvSysid).append("</rcv_sysid>\n");
        sb.append("<re_snd_yn>N</re_snd_yn>\n");
        sb.append("</file_info>\n");
        sb.append("<message>\n");
        sb.append("<layout_version>1.1</layout_version>\n");
        sb.append("<data>\n");
        sb.append(makeTime).append(makeTime).append("\n");
        sb.append("</data>\n");
        sb.append("<binary>\n");
        sb.append(jFileName).append("\n");
        sb.append("</binary>\n");
        sb.append("<total_data_row_num>30</total_data_row_num>\n");
        sb.append("</message>\n");
        sb.append("</xml_file>");
        return sb.toString();
    }
}
