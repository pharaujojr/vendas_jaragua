package com.example.vendasjaragua.service;

import com.example.vendasjaragua.model.Venda;
import com.example.vendasjaragua.repository.VendaRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import java.util.Collections;
import com.example.vendasjaragua.model.VendaItem;
import com.example.vendasjaragua.model.Produto;
import com.example.vendasjaragua.model.Time;
import com.example.vendasjaragua.model.Vendedor;
import com.example.vendasjaragua.repository.ProdutoRepository;
import com.example.vendasjaragua.repository.TimeRepository;
import com.example.vendasjaragua.repository.VendedorRepository;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ExcelService {

    private final VendaRepository vendaRepository;
    private final ProdutoRepository produtoRepository;
    private final TimeRepository timeRepository;
    private final VendedorRepository vendedorRepository;

    public void save(MultipartFile file) {
        try {
            List<Venda> vendas = parseExcelFile(file.getInputStream());
            vendaRepository.saveAll(vendas);
            vendaRepository.deleteEmptyRows(); // Cleanup after import
        } catch (IOException e) {
            throw new RuntimeException("fail to store excel data: " + e.getMessage());
        }
    }

    public List<Venda> parseExcelFile(InputStream is) {
        try {
            Workbook workbook = new XSSFWorkbook(is);
            Sheet sheet = workbook.getSheetAt(0); // Assume first sheet
            List<Venda> vendas = new ArrayList<>();

            int rowIndex = 0;
            for (Row row : sheet) {
                if (rowIndex == 0) { // Skip header
                    rowIndex++;
                    continue;
                }
                
                // Skip completely empty rows during parsing to save resource
                if (isRowEmpty(row)) {
                     rowIndex++;
                     continue;
                }

                Venda venda = new Venda();
                // Column mapping:
                // 0: CLIENTE, 1: NF, 2: OV, 3: ENTREGA, 4: TELEFONE, 5: CIDADE
                // 6: ESTADO, 7: VENDEDOR, 8: DATA, 9: PLACAS, 10: INVERSOR, 11: POTÊNCIA
                // 12: R$ VENDA, 13: R$ MATERIAL, 14: R$ BRUTO, 15: MARKUP, 16: PRODUTO, 17: TIME

                venda.setCliente(getStringValue(row.getCell(0)));
                venda.setNf(getStringValue(row.getCell(1)));
                venda.setOv(getStringValue(row.getCell(2)));
                venda.setEntrega(getStringValue(row.getCell(3)));
                venda.setTelefone(getStringValue(row.getCell(4)));
                venda.setCidade(getStringValue(row.getCell(5)));
                venda.setEstado(getStringValue(row.getCell(6)));
                venda.setVendedor(getStringValue(row.getCell(7)));
                
                // DATA
                venda.setData(getDateValue(row.getCell(8)));

                venda.setPlacas(getStringValue(row.getCell(9)));
                venda.setInversor(getStringValue(row.getCell(10)));
                venda.setPotencia(getStringValue(row.getCell(11)));

                venda.setValorVenda(getBigDecimalValue(row.getCell(12)));
                venda.setValorMaterial(getBigDecimalValue(row.getCell(13)));
                // campos calculados automaticamente pelo modelo: row.getCell(14) e (15) sao ignorados.
                
                String rawProduto = getStringValue(row.getCell(16));
                if (rawProduto != null && !rawProduto.trim().isEmpty()) {
                    VendaItem item = new VendaItem();
                    item.setNomeProduto(rawProduto);
                    item.setQuantidade(1);
                    item.setValorUnitarioVenda(BigDecimal.ZERO); // Valor desconhecido na importação simples
                    item.setValorUnitarioCusto(BigDecimal.ZERO);
                    venda.setProduto(new ArrayList<>(Collections.singletonList(item)));
                } else {
                    venda.setProduto(new ArrayList<>());
                }
                
                venda.setInverterInfo(new ArrayList<>()); // Initialize with empty list as requested

                venda.setTime(getStringValue(row.getCell(17)));

                vendas.add(venda);
                rowIndex++;
            }
            workbook.close();
            return vendas;
        } catch (IOException e) {
            throw new RuntimeException("fail to parse Excel file: " + e.getMessage());
        }
    }

    public void saveProdutos(MultipartFile file) {
        try {
            List<Produto> produtos = parseProdutoExcelFile(file.getInputStream());
            produtoRepository.saveAll(produtos);
        } catch (IOException e) {
            throw new RuntimeException("fail to store excel data: " + e.getMessage());
        }
    }

    public void saveTimes(MultipartFile file) {
        try {
            List<Time> times = parseTimeExcelFile(file.getInputStream());
            timeRepository.saveAll(times);
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public void saveVendedores(MultipartFile file) {
        try {
            List<Vendedor> vendedores = parseVendedorExcelFile(file.getInputStream());
            vendedorRepository.saveAll(vendedores);
        } catch (IOException e) {
            throw new RuntimeException("fail to store excel data: " + e.getMessage());
        }
    }

    public List<Produto> parseProdutoExcelFile(InputStream is) {
         try {
            Workbook workbook = new XSSFWorkbook(is);
            Sheet sheet = workbook.getSheetAt(0);
            List<Produto> produtos = new ArrayList<>();
            
            int rowIndex = 0;
            for (Row row : sheet) {
                if (rowIndex == 0) { // Skip header
                    rowIndex++;
                    continue;
                }
                if (isRowEmpty(row)) {
                     rowIndex++;
                     continue;
                }
                
                // DESCRIÇÃO|GRUPO|UNIDADE -> 0, 1, 2
                Produto p = new Produto();
                p.setDescricao(getStringValue(row.getCell(0)));
                p.setGrupo(getStringValue(row.getCell(1)));
                p.setUnidade(getStringValue(row.getCell(2)));
                
                produtos.add(p);
                rowIndex++;
            }
            workbook.close();
            return produtos;
        } catch (IOException e) {
            throw new RuntimeException("fail to parse Excel file: " + e.getMessage());
        }
    }

    public List<Time> parseTimeExcelFile(InputStream is) {
        try {
            Workbook workbook = new XSSFWorkbook(is);
            Sheet sheet = workbook.getSheetAt(0);
            List<Time> times = new ArrayList<>();
            
            int rowIndex = 0;
            for (Row row : sheet) {
                if (rowIndex == 0) { rowIndex++; continue; } // Skip Header
                if (isRowEmpty(row)) { rowIndex++; continue; }

                String nome = getStringValue(row.getCell(0));
                String lider = getStringValue(row.getCell(1));

                if (lider == null || lider.trim().isEmpty()) {
                    throw new IOException("Linha " + (rowIndex + 1) + ": Time '" + nome + "' não possui líder informado. Importação cancelada.");
                }

                // Check if exists
                Time time = timeRepository.findByNome(nome).stream().findFirst().orElse(new Time());
                time.setNome(nome);
                time.setLider(lider);
                
                times.add(time);
                rowIndex++;
            }
            workbook.close();
            return times;
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public List<Vendedor> parseVendedorExcelFile(InputStream is) {
        try {
            Workbook workbook = new XSSFWorkbook(is);
            Sheet sheet = workbook.getSheetAt(0);
            List<Vendedor> vendedores = new ArrayList<>();
            
            // Allow duplicate logic: we want to update existing by name or create new?
            // "importar" usually implies create if not exists
            
            int rowIndex = 0;
            for (Row row : sheet) {
                if (rowIndex == 0) { rowIndex++; continue; }
                if (isRowEmpty(row)) { rowIndex++; continue; }

                String nome = getStringValue(row.getCell(0));
                String nomeTime = getStringValue(row.getCell(1));

                Vendedor vendedor = vendedorRepository.findByNome(nome).stream().findFirst().orElse(new Vendedor());
                vendedor.setNome(nome);

                if (nomeTime != null && !nomeTime.trim().isEmpty()) {
                    Time time = timeRepository.findByNome(nomeTime).stream().findFirst().orElse(null);
                    vendedor.setTime(time);
                } else {
                    vendedor.setTime(null);
                }
                
                vendedores.add(vendedor);
                rowIndex++;
            }
            workbook.close();
            return vendedores;
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    private boolean isRowEmpty(Row row) {
        if (row == null) return true;
        // Check critical columns (CLIENTE=0, DATA=8, VALOR=12)
        // If all these are missing, we consider the row empty.
        Cell c0 = row.getCell(0);
        Cell c8 = row.getCell(8);
        Cell c12 = row.getCell(12);

        boolean c0Empty = (c0 == null || c0.getCellType() == CellType.BLANK);
        boolean c8Empty = (c8 == null || c8.getCellType() == CellType.BLANK);
        boolean c12Empty = (c12 == null || c12.getCellType() == CellType.BLANK);
        
        return c0Empty && c8Empty && c12Empty;
    }

    private String getStringValue(Cell cell) {
        if (cell == null) return null;
        switch (cell.getCellType()) {
            case STRING: return cell.getStringCellValue();
            case NUMERIC: return String.valueOf((long) cell.getNumericCellValue()); // integer string mostly
            case BOOLEAN: return String.valueOf(cell.getBooleanCellValue());
            default: return null;
        }
    }

    private Double getDoubleValue(Cell cell) {
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC) {
            return cell.getNumericCellValue();
        } else if (cell.getCellType() == CellType.STRING) {
             try {
                 return Double.parseDouble(cell.getStringCellValue().replace("R$", "").replace(",", ".").trim());
             } catch (NumberFormatException e) {
                 return null;
             }
        }
        return null;
    }

    private BigDecimal getBigDecimalValue(Cell cell) {
        Double val = getDoubleValue(cell);
        return val != null ? BigDecimal.valueOf(val) : null;
    }

    private java.time.LocalDate getDateValue(Cell cell) {
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getDateCellValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        }
        return null; // fallback or parse string if needed
    }
}
