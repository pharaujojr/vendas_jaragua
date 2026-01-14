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

@Service
@RequiredArgsConstructor
public class ExcelService {

    private final VendaRepository vendaRepository;

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
                venda.setProduto(getStringValue(row.getCell(16)));
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
