package com.example.vendasjaragua.controller;

import com.example.vendasjaragua.model.Venda;
import com.example.vendasjaragua.model.Time;
import com.example.vendasjaragua.model.Vendedor;
import com.example.vendasjaragua.repository.TimeRepository;
import com.example.vendasjaragua.repository.VendedorRepository;
import com.example.vendasjaragua.repository.VendaRepository;
import com.example.vendasjaragua.service.ExcelService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;

import java.util.List;

@RestController
@RequestMapping("/api/vendas")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // Allow for simple testing from static file
public class VendaController {

    private final ExcelService excelService;
    private final VendaRepository vendaRepository;
    private final TimeRepository timeRepository;
    private final VendedorRepository vendedorRepository;

    @PostMapping
    public ResponseEntity<Venda> createVenda(@RequestBody Venda venda) {
        try {
            Venda novaVenda = vendaRepository.save(venda);
            return new ResponseEntity<>(novaVenda, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/times")
    public ResponseEntity<List<Time>> getAllTimes() {
        return new ResponseEntity<>(timeRepository.findAll(), HttpStatus.OK);
    }

    @GetMapping("/vendedores")
    public ResponseEntity<List<Vendedor>> getAllVendedores() {
        return new ResponseEntity<>(vendedorRepository.findAll(), HttpStatus.OK);
    }

    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) {
        String message = "";
        if (ExcelHelper.hasExcelFormat(file)) {
            try {
                excelService.save(file);
                message = "Uploaded the file successfully: " + file.getOriginalFilename();
                return ResponseEntity.status(HttpStatus.OK).body(message);
            } catch (Exception e) {
                message = "Could not upload the file: " + file.getOriginalFilename() + ". Error: " + e.getMessage();
                return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).body(message);
            }
        }
        message = "Please upload an excel file!";
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(message);
    }

    @GetMapping
    public ResponseEntity<Page<Venda>> getAllVendas(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        try {
            // Sort by data descending by default to show newest first
            Pageable pageable = PageRequest.of(page, size, Sort.by("data").descending());
            Page<Venda> vendas;

            if (startDate != null && endDate != null) {
                vendas = vendaRepository.findByDataBetween(startDate, endDate, pageable);
            } else {
                vendas = vendaRepository.findAll(pageable);
            }

            if (vendas.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
            return new ResponseEntity<>(vendas, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}

class ExcelHelper {
    public static boolean hasExcelFormat(MultipartFile file) {
        return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet".equals(file.getContentType()) || 
               "application/vnd.ms-excel".equals(file.getContentType());
    }
}
