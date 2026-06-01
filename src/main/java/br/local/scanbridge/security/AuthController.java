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
    private final PasswordResetService passwordResetService;

    public AuthController(RegistrationService registrationService, PasswordResetService passwordResetService) {
        this.registrationService = registrationService;
        this.passwordResetService = passwordResetService;
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
            redirectAttributes.addFlashAttribute("error", "Informe um e-mail valido e senha com pelo menos 8 caracteres.");
            return "redirect:/register";
        }

        try {
            registrationService.register(registrationRequest);
        } catch (EmailAlreadyRegisteredException exception) {
            redirectAttributes.addFlashAttribute("registrationRequest", registrationRequest);
            redirectAttributes.addFlashAttribute("error", "Este e-mail ja esta cadastrado.");
            return "redirect:/register";
        }

        redirectAttributes.addFlashAttribute("success", "Cadastro criado. Entre com seu e-mail e senha.");
        return "redirect:/login";
    }

    @GetMapping("/forgot-password")
    public String forgotPassword(Model model) {
        if (!model.containsAttribute("forgotPasswordRequest")) {
            model.addAttribute("forgotPasswordRequest", new ForgotPasswordRequest());
        }
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String forgotPassword(
            @Valid @ModelAttribute ForgotPasswordRequest forgotPasswordRequest,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes
    ) {
        if (!bindingResult.hasErrors()) {
            passwordResetService.requestReset(forgotPasswordRequest);
        }
        redirectAttributes.addFlashAttribute("success", "Se o e-mail existir, enviaremos um link de recuperacao.");
        return "redirect:/forgot-password";
    }

    @GetMapping("/reset-password")
    public String resetPassword(@ModelAttribute ResetPasswordRequest resetPasswordRequest) {
        return "reset-password";
    }

    @PostMapping("/reset-password")
    public String resetPassword(
            @Valid @ModelAttribute ResetPasswordRequest resetPasswordRequest,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Informe uma senha com pelo menos 8 caracteres.");
            redirectAttributes.addAttribute("token", resetPasswordRequest.getToken());
            return "redirect:/reset-password";
        }

        boolean reset = passwordResetService.resetPassword(resetPasswordRequest);
        if (!reset) {
            redirectAttributes.addFlashAttribute("error", "Link invalido ou expirado.");
            return "redirect:/forgot-password";
        }

        redirectAttributes.addFlashAttribute("success", "Senha alterada. Entre com sua nova senha.");
        return "redirect:/login";
    }
}
