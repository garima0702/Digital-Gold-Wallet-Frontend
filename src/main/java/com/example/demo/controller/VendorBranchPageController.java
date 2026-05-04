package com.example.demo.controller;

import com.example.demo.service.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
@RequestMapping("/vendor-branches")
public class VendorBranchPageController {

    private final VendorBranchService vendorBranchService;
    private final PhysicalGoldTransactionService physicalGoldTransactionService;
    private final VirtualGoldHoldingService virtualGoldHoldingService;
    private final TransactionHistoryService transactionHistoryService;

    public VendorBranchPageController(
            VendorBranchService vendorBranchService,
            PhysicalGoldTransactionService physicalGoldTransactionService,
            VirtualGoldHoldingService virtualGoldHoldingService,
            TransactionHistoryService transactionHistoryService
    ) {
        this.vendorBranchService = vendorBranchService;
        this.physicalGoldTransactionService = physicalGoldTransactionService;
        this.virtualGoldHoldingService = virtualGoldHoldingService;
        this.transactionHistoryService = transactionHistoryService;
    }

    @GetMapping
    public String listBranches(@RequestParam String member,
                               @RequestParam(required = false) String searchType,
                               @RequestParam(required = false) String keyword,
                               @RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "10") int size,
                               Model model) {

        Map<String, Object> pagedResult;

        if (keyword == null || keyword.isBlank()) {
            pagedResult = vendorBranchService.getAllVendorBranchesPaginated(page, size);
        } else if ("vendorId".equalsIgnoreCase(searchType)) {
            pagedResult = vendorBranchService.findByVendorIdPaginated(Integer.parseInt(keyword), page, size);
        } else if ("city".equalsIgnoreCase(searchType)) {
            pagedResult = vendorBranchService.findByCityPaginated(keyword, page, size);
        } else if ("state".equalsIgnoreCase(searchType)) {
            pagedResult = vendorBranchService.findByStatePaginated(keyword, page, size);
        } else {
            pagedResult = vendorBranchService.getAllVendorBranchesPaginated(page, size);
        }

        int currentPage = (Integer) pagedResult.get("number");
        int totalPages = (Integer) pagedResult.get("totalPages");

        model.addAttribute("member", member);
        model.addAttribute("searchType", searchType);
        model.addAttribute("keyword", keyword);
        model.addAttribute("branches", pagedResult.get("data"));

        model.addAttribute("currentPage", currentPage);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalElements", pagedResult.get("totalElements"));
        model.addAttribute("size", size);
        model.addAttribute("hasPrevious", currentPage > 0);
        model.addAttribute("hasNext", currentPage + 1 < totalPages);

        return "vendor-branches/list";
    }

    @GetMapping("/{branchId}")
    public String branchDetails(@PathVariable Integer branchId,
                                @RequestParam String member,
                                Model model) {

        Map<String, Object> branch = vendorBranchService.getVendorBranchById(branchId);
        Map<String, Object> vendor = vendorBranchService.getLinkedVendor(branch);

        model.addAttribute("member", member);
        model.addAttribute("branch", branch);
        model.addAttribute("vendor", vendor);

        switch (member.toLowerCase()) {
            case "shivam":
                model.addAttribute("physicalTransactions",
                        physicalGoldTransactionService.getTransactionsByBranchId(branchId));
                break;

            case "shruti":
                model.addAttribute("holdings",
                        virtualGoldHoldingService.getHoldingsByBranchId(branchId));

                model.addAttribute("transactions",
                        transactionHistoryService.getTransactionsByBranchId(branchId));
                break;
        }

        return "vendor-branches/details";
    }

    @GetMapping("/create")
    public String createBranchForm(@RequestParam String member, Model model) {

        model.addAttribute("member", member);
        model.addAttribute("vendors", vendorBranchService.getAllVendors());
        model.addAttribute("addresses", vendorBranchService.getAllAddresses());

        return "vendor-branches/create";
    }

    @PostMapping("/create")
    public String createBranch(@RequestParam Integer vendorId,
                               @RequestParam Integer addressId,
                               @RequestParam String quantity,
                               @RequestParam String member) {

        vendorBranchService.createBranch(vendorId, addressId, quantity);

        return "redirect:/vendor-branches?member=" + member;
    }

    @GetMapping("/update/{branchId}")
    public String updateBranchForm(@PathVariable Integer branchId,
                                   @RequestParam String member,
                                   Model model) {

        model.addAttribute("member", member);
        model.addAttribute("branch", vendorBranchService.getVendorBranchById(branchId));

        return "vendor-branches/update";
    }

    @PostMapping("/update/{branchId}")
    public String updateBranch(@PathVariable Integer branchId,
                               @RequestParam String quantity,
                               @RequestParam String member) {

        vendorBranchService.updateBranch(branchId, quantity);

        return "redirect:/vendor-branches?member=" + member;
    }
}