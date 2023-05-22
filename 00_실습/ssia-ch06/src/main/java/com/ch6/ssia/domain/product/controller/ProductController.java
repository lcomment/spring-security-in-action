package com.ch6.ssia.domain.product.controller;

import com.ch6.ssia.domain.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping("/main")
    public String main(Authentication authentication, Model model) {
        model.addAttribute( "username", authentication.getName());
        model.addAttribute( "products", productService.findAll());

        return "main.html";
    }
}
