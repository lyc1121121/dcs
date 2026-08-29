package com.dcsmanager.controller;

import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 158단계: PRIVATE 탭에 "파일탐색기" 추가 - 실제 CentOS 서버의 /working 아래를 읽기전용으로
 * 훑어볼 수 있게 한다(파일질라처럼 폴더 이동 + 파일 클릭해서 내용 확인).
 *
 * DCSManager 는 도커 컨테이너 안에서 돌기 때문에 원래는 호스트(CentOS) 파일시스템을 볼 수
 * 없다 - 이 기능을 위해 dcsmanager-test-tomcat 컨테이너에 호스트의 /working 을 읽기전용으로
 * 마운트해서 컨테이너 안에서 /host-working 경로로 보이게 해뒀다(전체 호스트 루트가 아니라
 * /working 하나만 - 사용자가 직접 이 범위로 선택함).
 *
 * 보안: ROOT 밖으로 못 벗어나게(경로 조작 방지) toRealPath() 로 심볼릭 링크까지 실제 경로로
 * 풀어낸 뒤 ROOT 하위인지 매번 검증한다. 쓰기/삭제/업로드 기능은 없음(조회 전용).
 */
@RestController
@RequestMapping("/private/files")
public class FileBrowserController {

    private static final String SESSION_KEY = "privateAuthenticated";
    private static final Path ROOT = Paths.get("/host-working");
    private static final long MAX_PREVIEW_BYTES = 10_000_000; // 10MB 넘으면 앞부분만 보여줌
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final List<String> IMAGE_EXT = Arrays.asList("jpg", "jpeg", "png", "gif", "webp", "bmp", "svg");
    private static final List<String> PDF_EXT = Arrays.asList("pdf");
    private static final List<String> PPTX_EXT = Arrays.asList("pptx");
    private static final List<String> DOCX_EXT = Arrays.asList("docx");
    private static final int MAX_OFFICE_TEXT_CHARS = 200_000;
    private static final List<String> BINARY_EXT = Arrays.asList(
            "jar", "war", "class", "zip", "tar", "gz", "ppt", "xlsx", "xls",
            "doc", "db", "mv", "so", "exe", "bin");

    // 158단계 후속: pptx/docx 원본 그대로(서식·이미지 포함)의 모습을 보여주기 위해
    // 컨테이너에 설치한 LibreOffice로 PDF 변환 후, 이미 만들어둔 PDF 미리보기(iframe)로
    // 재사용한다. 변환 결과는 캐시해서 같은 파일을 다시 열 때는 재변환하지 않는다.
    private static final Path OFFICE_CACHE_DIR = Paths.get("/tmp/office-preview-cache");
    private static final Object CONVERT_LOCK = new Object();
    private static final long CONVERT_TIMEOUT_SECONDS = 60;

    private boolean isAuthenticated(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session != null && Boolean.TRUE.equals(session.getAttribute(SESSION_KEY));
    }

    /**
     * 요청받은 상대경로를 ROOT 기준 실제 경로로 안전하게 풀어낸다.
     * ROOT 밖을 가리키면(경로 조작 시도 포함) null 을 반환한다.
     */
    private Path resolveSafe(String relativePath) {
        String cleaned = relativePath == null ? "" : relativePath.replace("\\", "/");
        while (cleaned.startsWith("/")) {
            cleaned = cleaned.substring(1);
        }
        Path candidate = ROOT.resolve(cleaned).normalize();
        try {
            Path realRoot = ROOT.toRealPath();
            Path realCandidate = Files.exists(candidate) ? candidate.toRealPath() : candidate.normalize();
            if (!realCandidate.equals(realRoot) && !realCandidate.startsWith(realRoot.toString() + "/")) {
                return null;
            }
            return realCandidate;
        } catch (IOException e) {
            return null;
        }
    }

    @GetMapping
    public Map<String, Object> list(@RequestParam(defaultValue = "") String path, HttpServletRequest request) {
        if (!isAuthenticated(request)) {
            return errorBody("unauthorized");
        }
        Path dir = resolveSafe(path);
        if (dir == null || !Files.isDirectory(dir)) {
            return errorBody("invalid path");
        }

        List<Map<String, Object>> entries = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path p : stream) {
                Map<String, Object> e = new HashMap<>();
                boolean isDir = Files.isDirectory(p);
                e.put("name", p.getFileName().toString());
                e.put("dir", isDir);
                e.put("size", isDir ? null : safeSize(p));
                e.put("modified", safeModified(p));
                entries.add(e);
            }
        } catch (IOException e) {
            return errorBody("읽을 수 없습니다: " + e.getMessage());
        }
        entries.sort(Comparator
                .comparing((Map<String, Object> e) -> !(Boolean) e.get("dir"))
                .thenComparing(e -> ((String) e.get("name")).toLowerCase()));

        Map<String, Object> body = new HashMap<>();
        String relPath = ROOT.toAbsolutePath().relativize(dir.toAbsolutePath()).toString();
        body.put("ok", true);
        body.put("path", relPath.equals(".") ? "" : relPath);
        body.put("entries", entries);
        return body;
    }

    @GetMapping("/content")
    public Map<String, Object> content(@RequestParam String path, HttpServletRequest request) {
        if (!isAuthenticated(request)) {
            return errorBody("unauthorized");
        }
        Path file = resolveSafe(path);
        if (file == null || !Files.isRegularFile(file)) {
            return errorBody("invalid path");
        }

        String name = file.getFileName().toString();
        String ext = extensionOf(name);
        long size;
        try {
            size = Files.size(file);
        } catch (IOException e) {
            return errorBody("크기를 확인할 수 없습니다");
        }

        Map<String, Object> body = new HashMap<>();
        body.put("ok", true);
        body.put("name", name);
        body.put("size", size);

        if (IMAGE_EXT.contains(ext)) {
            body.put("type", "image");
            body.put("imageUrl", "/private/files/raw?path=" + urlEncode(path));
            return body;
        }
        if (PDF_EXT.contains(ext)) {
            body.put("type", "pdf");
            body.put("pdfUrl", "/private/files/raw?path=" + urlEncode(path));
            return body;
        }
        if (PPTX_EXT.contains(ext) || DOCX_EXT.contains(ext)) {
            try {
                convertToPdfCached(file, path);
                body.put("type", "pdf");
                body.put("pdfUrl", "/private/files/office-pdf?path=" + urlEncode(path));
                return body;
            } catch (Exception e) {
                // LibreOffice 변환 실패 시 텍스트 추출로 대체(서식은 없지만 내용은 확인 가능)
                return PPTX_EXT.contains(ext) ? extractPptx(file, body) : extractDocx(file, body);
            }
        }
        if (BINARY_EXT.contains(ext)) {
            body.put("type", "unsupported");
            return body;
        }
        try {
            byte[] bytes = Files.readAllBytes(file);
            boolean truncated = bytes.length > MAX_PREVIEW_BYTES;
            byte[] slice = truncated ? java.util.Arrays.copyOf(bytes, (int) MAX_PREVIEW_BYTES) : bytes;
            String text = new String(slice, StandardCharsets.UTF_8);
            body.put("type", "text");
            body.put("truncated", truncated);
            body.put("text", text);
        } catch (IOException e) {
            body.put("type", "unsupported");
        }
        return body;
    }

    /** 이미지/PDF 파일의 실제 바이트를 그대로 내려준다(미리보기용, <img>/<iframe> 에서 사용). */
    @GetMapping("/raw")
    public void raw(@RequestParam String path, HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!isAuthenticated(request)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        Path file = resolveSafe(path);
        if (file == null || !Files.isRegularFile(file)) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        String ext = extensionOf(file.getFileName().toString());
        String contentType;
        if (IMAGE_EXT.contains(ext)) {
            contentType = "svg".equals(ext) ? "image/svg+xml" : "image/" + ("jpg".equals(ext) ? "jpeg" : ext);
        } else if (PDF_EXT.contains(ext)) {
            contentType = "application/pdf";
        } else {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        response.setContentType(contentType);
        try (OutputStream out = response.getOutputStream()) {
            Files.copy(file, out);
        }
    }

    /** pptx/docx 를 LibreOffice로 변환한 PDF(캐시됨)를 그대로 내려준다(원본 서식·이미지 포함). */
    @GetMapping("/office-pdf")
    public void officePdf(@RequestParam String path, HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!isAuthenticated(request)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        Path file = resolveSafe(path);
        if (file == null || !Files.isRegularFile(file)) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        String ext = extensionOf(file.getFileName().toString());
        if (!PPTX_EXT.contains(ext) && !DOCX_EXT.contains(ext)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        Path pdf;
        try {
            pdf = convertToPdfCached(file, path);
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }
        response.setContentType("application/pdf");
        try (OutputStream out = response.getOutputStream()) {
            Files.copy(pdf, out);
        }
    }

    /**
     * pptx/docx 파일을 LibreOffice(soffice --headless)로 PDF 변환해 캐시 디렉터리에 저장하고
     * 그 경로를 반환한다. 원본 상대경로 + 수정시각을 캐시 키로 써서, 파일이 바뀌지 않는 한
     * 같은 파일을 다시 열 때는 재변환하지 않는다. 동시에 여러 변환이 들어와도 LibreOffice
     * 프로세스끼리 충돌하지 않도록 전역 락으로 직렬화한다(내부용 저용량 도구라 충분함).
     */
    private Path convertToPdfCached(Path sourceFile, String relativePath) throws IOException, InterruptedException {
        Files.createDirectories(OFFICE_CACHE_DIR);
        long mtime = Files.getLastModifiedTime(sourceFile).toMillis();
        String cacheKey = UUID.nameUUIDFromBytes((relativePath + "_" + mtime).getBytes(StandardCharsets.UTF_8)).toString();
        Path cachedPdf = OFFICE_CACHE_DIR.resolve(cacheKey + ".pdf");
        if (Files.exists(cachedPdf)) {
            return cachedPdf;
        }
        synchronized (CONVERT_LOCK) {
            if (Files.exists(cachedPdf)) {
                return cachedPdf;
            }
            Path workDir = Files.createTempDirectory(OFFICE_CACHE_DIR, "conv-");
            Path profileDir = Files.createTempDirectory(OFFICE_CACHE_DIR, "profile-");
            try {
                ProcessBuilder pb = new ProcessBuilder(
                        "soffice", "--headless", "--norestore", "--nolockcheck", "--nodefault", "--nofirststartwizard",
                        "-env:UserInstallation=file://" + profileDir.toAbsolutePath(),
                        "--convert-to", "pdf", "--outdir", workDir.toAbsolutePath().toString(),
                        sourceFile.toAbsolutePath().toString());
                pb.redirectErrorStream(true);
                Process proc = pb.start();
                try (InputStream is = proc.getInputStream()) {
                    byte[] buf = new byte[4096];
                    while (is.read(buf) != -1) {
                        // 변환 로그는 버려서 파이프가 막혀 프로세스가 멈추지 않게만 한다
                    }
                }
                boolean finished = proc.waitFor(CONVERT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                if (!finished) {
                    proc.destroyForcibly();
                    throw new IOException("LibreOffice 변환 시간 초과");
                }
                String baseName = sourceFile.getFileName().toString();
                int dot = baseName.lastIndexOf('.');
                String outName = (dot > 0 ? baseName.substring(0, dot) : baseName) + ".pdf";
                Path produced = workDir.resolve(outName);
                if (!Files.exists(produced)) {
                    throw new IOException("LibreOffice 변환 결과 파일이 없습니다");
                }
                Files.move(produced, cachedPdf, StandardCopyOption.REPLACE_EXISTING);
                return cachedPdf;
            } finally {
                deleteRecursive(workDir);
                deleteRecursive(profileDir);
            }
        }
    }

    private static void deleteRecursive(Path dir) {
        if (dir == null || !Files.exists(dir)) {
            return;
        }
        try (java.util.stream.Stream<Path> walk = Files.walk(dir)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException ignored) {
                    // 임시 디렉터리 정리 실패는 무시(캐시 디스크가 차지 않도록 최선만 다함)
                }
            });
        } catch (IOException ignored) {
            // 정리 실패는 무시
        }
    }

    /**
     * pptx 슬라이드별 텍스트만 뽑는다(Apache POI 사용) - LibreOffice 변환이 실패했을 때의
     * 대체 경로. 서식/이미지는 없지만 내용 확인은 가능하다.
     */
    private Map<String, Object> extractPptx(Path file, Map<String, Object> body) {
        StringBuilder sb = new StringBuilder();
        try (InputStream in = Files.newInputStream(file); XMLSlideShow ppt = new XMLSlideShow(in)) {
            List<XSLFSlide> slides = ppt.getSlides();
            for (int i = 0; i < slides.size(); i++) {
                sb.append("=== 슬라이드 ").append(i + 1).append(" ===\n");
                for (XSLFShape shape : slides.get(i).getShapes()) {
                    if (shape instanceof XSLFTextShape) {
                        String t = ((XSLFTextShape) shape).getText();
                        if (t != null && !t.trim().isEmpty()) {
                            sb.append(t.trim()).append("\n");
                        }
                    }
                }
                sb.append("\n");
                if (sb.length() > MAX_OFFICE_TEXT_CHARS) {
                    sb.append("... (내용이 길어 이후 슬라이드는 생략합니다)");
                    break;
                }
            }
        } catch (Exception e) {
            body.put("type", "unsupported");
            return body;
        }
        body.put("type", "office");
        body.put("text", sb.toString());
        return body;
    }

    /** docx 는 문단(및 표) 텍스트만 순서대로 뽑아 보여준다(Apache POI 사용, 서식은 제외). */
    private Map<String, Object> extractDocx(Path file, Map<String, Object> body) {
        StringBuilder sb = new StringBuilder();
        try (InputStream in = Files.newInputStream(file); XWPFDocument doc = new XWPFDocument(in)) {
            for (org.apache.poi.xwpf.usermodel.IBodyElement el : doc.getBodyElements()) {
                if (el instanceof XWPFParagraph) {
                    String t = ((XWPFParagraph) el).getText();
                    if (t != null && !t.trim().isEmpty()) {
                        sb.append(t).append("\n");
                    }
                } else if (el instanceof XWPFTable) {
                    for (org.apache.poi.xwpf.usermodel.XWPFTableRow row : ((XWPFTable) el).getRows()) {
                        List<String> cells = new ArrayList<>();
                        for (org.apache.poi.xwpf.usermodel.XWPFTableCell cell : row.getTableCells()) {
                            cells.add(cell.getText());
                        }
                        sb.append(String.join(" | ", cells)).append("\n");
                    }
                }
                if (sb.length() > MAX_OFFICE_TEXT_CHARS) {
                    sb.append("... (내용이 길어 이후는 생략합니다)");
                    break;
                }
            }
        } catch (Exception e) {
            body.put("type", "unsupported");
            return body;
        }
        body.put("type", "office");
        body.put("text", sb.toString());
        return body;
    }

    private static String extensionOf(String name) {
        int i = name.lastIndexOf('.');
        return i < 0 ? "" : name.substring(i + 1).toLowerCase();
    }

    private static Long safeSize(Path p) {
        try {
            return Files.size(p);
        } catch (IOException e) {
            return null;
        }
    }

    private static String safeModified(Path p) {
        try {
            Instant instant = Files.getLastModifiedTime(p).toInstant();
            return DT_FMT.format(instant.atZone(ZoneId.of("Asia/Seoul")));
        } catch (IOException e) {
            return null;
        }
    }

    private static String urlEncode(String s) {
        try {
            return java.net.URLEncoder.encode(s, "UTF-8");
        } catch (Exception e) {
            return s;
        }
    }

    private Map<String, Object> errorBody(String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("ok", false);
        body.put("message", message);
        return body;
    }
}
