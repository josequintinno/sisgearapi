package br.com.digital.quintino.sisgearapi.controller;

import br.com.digital.quintino.sisgearapi.configuration.ArquivoConfiguration;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/api/arquivo")
public class ArquivoController {

    private final Path caminhoArquivo;

    public ArquivoController(ArquivoConfiguration arquivoConfiguration) {
        this.caminhoArquivo = Paths.get(arquivoConfiguration.getDiretorio()).toAbsolutePath().normalize();
    }

    @PostMapping("/upload")
    public ResponseEntity<String> uploadArquivo(@RequestParam("arquivo") MultipartFile arquivo) {
        String nomeArquivo = StringUtils.cleanPath(arquivo.getOriginalFilename());
        try {
            Path localizacaoArquivo = this.caminhoArquivo.resolve(nomeArquivo);
            arquivo.transferTo(localizacaoArquivo);
            return ResponseEntity.ok("Arquivo Salvo com sucesso [" + this.getLinkArquivo(nomeArquivo) + "]");
        } catch (IOException ioException) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/upload-multiplo")
    public ResponseEntity<String> uploadArquivo(@RequestParam("arquivo") List<MultipartFile> arquivoList) {
        for (MultipartFile arquivo : arquivoList ) {
            String nomeArquivo = StringUtils.cleanPath(arquivo.getOriginalFilename());
            try {
                Path localizacaoArquivo = this.caminhoArquivo.resolve(nomeArquivo);
                arquivo.transferTo(localizacaoArquivo);
            } catch (IOException ioException) {
                return ResponseEntity.badRequest().build();
            }
        }
        return ResponseEntity.ok("Arquivos Salvos com sucesso!");
    }

    @GetMapping("/download/{nomeArquivo:.+}")
    public ResponseEntity<Resource> downloadArquivo(@PathVariable String nomeArquivo, HttpServletRequest httpServletRequest) {
        Path path = this.caminhoArquivo.resolve(nomeArquivo).normalize();
        try {
            Resource resource = new UrlResource(path.toUri());
            String contentType = httpServletRequest.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
            return ResponseEntity.ok().contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\""
                            + resource.getFilename() + "\"").body(resource);
        } catch (IOException malformedURLException) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/diretorio")
    public ResponseEntity<List<String>> listaArquivos() throws IOException {
        List<String> arquivoList = Files.list(this.caminhoArquivo).map(Path::getFileName)
                .map(Path::toString)
                .collect(Collectors.toList());
        return ResponseEntity.ok(arquivoList);
    }

    private String getLinkArquivo(String nomeArquivo) {
        return ServletUriComponentsBuilder.fromCurrentContextPath().path("/api/arquivo/downloadArquivo/").path(nomeArquivo).build().toUriString();
    }

}
