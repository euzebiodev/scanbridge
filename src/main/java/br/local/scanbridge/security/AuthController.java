package br.local.scanbridge.security;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private final RegistrationService registrationService;

    public AuthController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String register(Model model) {
        if (!model.containsAttribute("registrationRequest")) {
            model.addAttribute("registrationRequest", new RegistrationRequest());
        }
        return "register";
    }

    @PostMapping("/register")
    public String register(
            @Valid @ModelAttribute RegistrationRequest registrationRequest,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("registrationRequest", registrationRequest);
            redirectAttributes.addFlashAttribute("error", "Informe um e-mail valido.");
            return "redirect:/register";
        }

        try {
            registrationService.register(registrationRequest);
        } catch (EmailAlreadyRegisteredException exception) {
            redirectAttributes.addFlashAttribute("registrationRequest", registrationRequest);
            redirectAttributes.addFlashAttribute("error", "Este e-mail ja esta cadastrado.");
            return "redirect:/register";
        }

        redirectAttributes.addFlashAttribute("success", "Cadastro criado. Entre com seu e-mail.");
        return "redirect:/login";
    }
}
