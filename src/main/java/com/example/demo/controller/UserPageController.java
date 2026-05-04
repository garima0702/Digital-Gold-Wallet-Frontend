package com.example.demo.controller;

import com.example.demo.service.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
@RequestMapping("/users")
public class UserPageController {

    private final UserService userService;
    private final PaymentService paymentService;
    private final TransactionHistoryService transactionHistoryService;
    private final VirtualGoldHoldingService virtualGoldHoldingService;
    private final PhysicalGoldTransactionService physicalGoldTransactionService;
    private final AddressService addressService;

    public UserPageController(UserService userService,
                              PaymentService paymentService,
                              TransactionHistoryService transactionHistoryService,
                              VirtualGoldHoldingService virtualGoldHoldingService,
                              PhysicalGoldTransactionService physicalGoldTransactionService,
                              AddressService addressService) {

        this.userService = userService;
        this.paymentService = paymentService;
        this.transactionHistoryService = transactionHistoryService;
        this.virtualGoldHoldingService = virtualGoldHoldingService;
        this.physicalGoldTransactionService = physicalGoldTransactionService;
        this.addressService = addressService;
    }

    @GetMapping
    public String listUsers(@RequestParam String member,
                            @RequestParam(required = false) String searchType,
                            @RequestParam(required = false) String keyword,
                            @RequestParam(defaultValue = "0") int page,
                            @RequestParam(defaultValue = "10") int size,
                            Model model) {

        Map<String, Object> pagedResult;

        if (keyword == null || keyword.isBlank()) {
            pagedResult = userService.getAllUsersPaginated(page, size);
        } else if ("name".equalsIgnoreCase(searchType)) {
            pagedResult = userService.findUsersByName(keyword, page, size);
        } else if ("city".equalsIgnoreCase(searchType)) {
            pagedResult = userService.findUsersByCity(keyword, page, size);
        } else if ("state".equalsIgnoreCase(searchType)) {
            pagedResult = userService.findUsersByState(keyword, page, size);
        } else {
            pagedResult = userService.getAllUsersPaginated(page, size);
        }

        int currentPage = (Integer) pagedResult.get("number");
        int totalPages = (Integer) pagedResult.get("totalPages");

        model.addAttribute("member", member);
        model.addAttribute("searchType", searchType);
        model.addAttribute("keyword", keyword);
        model.addAttribute("users", pagedResult.get("data"));

        model.addAttribute("currentPage", currentPage);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalElements", pagedResult.get("totalElements"));
        model.addAttribute("size", size);
        model.addAttribute("hasPrevious", currentPage > 0);
        model.addAttribute("hasNext", currentPage + 1 < totalPages);

        return "users/list";
    }

    @GetMapping("/{userId}")
    public String userDetails(@PathVariable Integer userId,
                              @RequestParam String member,
                              Model model) {

        Map<String, Object> user = userService.getUserById(userId);

        model.addAttribute("member", member);
        model.addAttribute("user", user);

        switch (member.toLowerCase()) {

            case "ashu":
                model.addAttribute("transactions",
                        transactionHistoryService.getTransactionsByUserId(userId));

                model.addAttribute("holdings",
                        virtualGoldHoldingService.getHoldingsByUserId(userId));
                break;

            case "vinayak":
                model.addAttribute("payments",
                        paymentService.getPaymentsByUserId(userId));

                model.addAttribute("address", getAddressFromEntityLinks(user));
                break;

            case "garima":
                model.addAttribute("physicalTransactions",
                        physicalGoldTransactionService.getTransactionsByUserId(userId));
                break;
        }

        return "users/details";
    }

    @GetMapping("/create")
    public String createUserForm(@RequestParam String member, Model model) {
        model.addAttribute("member", member);
        return "users/create";
    }

    @PostMapping("/create")
    public String createUser(@RequestParam String name,
                             @RequestParam String email,
                             @RequestParam String balance,
                             @RequestParam String member) {

        userService.createUser(name, email, balance);

        return "redirect:/users?member=" + member;
    }

    @GetMapping("/update/{userId}")
    public String updateUserForm(@PathVariable Integer userId,
                                 @RequestParam String member,
                                 Model model) {

        model.addAttribute("member", member);
        model.addAttribute("user", userService.getUserById(userId));

        return "users/update";
    }

    @PostMapping("/update/{userId}")
    public String updateUser(@PathVariable Integer userId,
                             @RequestParam String name,
                             @RequestParam String email,
                             @RequestParam String balance,
                             @RequestParam String member) {

        userService.updateUser(userId, name, email, balance);

        return "redirect:/users?member=" + member;
    }

    private Map<String, Object> getAddressFromEntityLinks(Map<String, Object> entity) {

        if (entity == null || entity.get("_links") == null) {
            return null;
        }

        Map<String, Object> links = (Map<String, Object>) entity.get("_links");

        if (links.get("address") == null) {
            return null;
        }

        Map<String, Object> addressLink = (Map<String, Object>) links.get("address");
        String addressUrl = (String) addressLink.get("href");

        if (addressUrl == null || addressUrl.isBlank()) {
            return null;
        }

        return addressService.getAddressByUrl(addressUrl);
    }
}