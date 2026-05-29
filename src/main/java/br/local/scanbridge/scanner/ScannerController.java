package br.local.scanbridge.scanner;

import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;

@Controller
public class ScannerController {

    private final ScannerService scannerService;

    public ScannerController(ScannerService scannerService) {
        this.scannerService = scannerService;
    }

    @GetMapping("/")
    public String index(Model model) throws IOException {
        if (!model.containsAttribute("scanRequest")) {
            model.addAttribute("scanRequest", new ScanRequest());
        }
        model.addAttribute("documents", scannerService.listDocuments());
        model.addAttribute("timeoutSeconds", scannerService.timeout().toSeconds());
        return "index";
    }

    @PostMapping("/scan")
    public String scan(
            @Valid @ModelAttribute ScanRequest scanRequest,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Revise as opcoes de digitalizacao.");
            return "redirect:/";
        }

        try {
            ScanDocument document = scannerService.scan(scanRequest);
            redirectAttributes.addFlashAttribute("success", "Documento digitalizado: " + document.fileName());
        } catch (IOException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            redirectAttributes.addFlashAttribute("error", "Digitalizacao interrompida.");
        }
        return "redirect:/";
    }

    @GetMapping("/documents/{fileName}")
    public ResponseEntity<Resource> download(@PathVariable String fileName) throws IOException {
        Resource resource = scannerService.load(fileName);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(fileName)
                        .build()
                        .toString())
                .body(resource);
    }
}
