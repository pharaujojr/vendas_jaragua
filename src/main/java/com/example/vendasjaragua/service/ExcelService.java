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
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@RequiredArgsConstructor
public class ExcelService {

    private static final Logger logger = LoggerFactory.getLogger(ExcelService.class);

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
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
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

                // Log raw values for debug
                Cell cellVal = row.getCell(12);
                Cell cellMat = row.getCell(13);
                
                if (rowIndex <= 10) { // Log only first 10 rows to avoid spam
                     logger.info("Row {}: Col 12 (Venda) Type={}; Col 13 (Mat) Type={}", 
                        rowIndex, 
                        cellVal != null ? cellVal.getCellType() : "NULL",
                        cellMat != null ? cellMat.getCellType() : "NULL"
                    );
                }

                BigDecimal valorVenda = getBigDecimalValue(cellVal, evaluator);
                venda.setValorVenda(valorVenda);
                BigDecimal valorMaterial = getBigDecimalValue(cellMat, evaluator);
                venda.setValorMaterial(valorMaterial); 
                // campos calculados automaticamente pelo modelo: row.getCell(14) e (15) sao ignorados.
                
                // Conforme solicitado, ignoramos a col 16 (Nome do produto) e usamos sempre "Não Especificado"
                // para garantir consistência e evitar problemas de agrupamento ou dados sujos.
                VendaItem itemNaoEspecificado = new VendaItem();
                itemNaoEspecificado.setNomeProduto("Não Especificado");
                itemNaoEspecificado.setQuantidade(1);
                
                // Use os valores das colunas 12 e 13 para Venda e Custo
                itemNaoEspecificado.setValorUnitarioVenda(valorVenda != null ? valorVenda : BigDecimal.ZERO);
                itemNaoEspecificado.setValorUnitarioCusto(valorMaterial != null ? valorMaterial : BigDecimal.ZERO);
                
                venda.setProduto(new ArrayList<>(Collections.singletonList(itemNaoEspecificado)));
                
                venda.setInverterInfo(new ArrayList<>()); // Initialize with empty list as requested

                String rawTime = getStringValue(row.getCell(17));
                venda.setTime(rawTime.isEmpty() ? null : rawTime);

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
        if (cell == null) return "";
        try {
            String value = "";
            switch (cell.getCellType()) {
                case STRING: value = cell.getStringCellValue(); break;
                case NUMERIC: value = String.valueOf((long) cell.getNumericCellValue()); break;
                case BOOLEAN: value = String.valueOf(cell.getBooleanCellValue()); break;
                default: value = "";
            }
            return value != null ? value.trim() : "";
        } catch (Exception e) {
            return "";
        }
    }

    private Double getDoubleValue(Cell cell, FormulaEvaluator evaluator) {
        if (cell == null) return 0.0;
        try {
            // First: If it's a formula, try to get the pre-calculated (cached) value if evaluation fails or returns error
            if (cell.getCellType() == CellType.FORMULA) {
                try {
                    // Start by assuming we might want the cached value if evaluation is tricky
                    // But usually we try evaluate first.
                    CellValue cellValue = evaluator.evaluate(cell);
                    
                    if (cellValue.getCellType() == CellType.NUMERIC) {
                        return cellValue.getNumberValue();
                    }
                    if (cellValue.getCellType() == CellType.ERROR) {
                        // Evaluation calculated an error (e.g. #DIV/0!), fallback to cached if valid
                         if (cell.getCachedFormulaResultType() == CellType.NUMERIC) {
                             return cell.getNumericCellValue();
                         }
                    }
                } catch (Exception e) {
                   // Evaluation failed (e.g. unsupported function), strictly fallback to cached
                   if (cell.getCachedFormulaResultType() == CellType.NUMERIC) {
                        return cell.getNumericCellValue();
                   }
                }
                // If evaluation resulted in string or failed all above, continue to formatter
            }

            // For numeric cells, return directly to avoid string parsing issues
             if (cell.getCellType() == CellType.NUMERIC && !DateUtil.isCellDateFormatted(cell)) {
                return cell.getNumericCellValue();
            }

            // Last resort: String parsing (handles currency "R$ 1.000,00" etc)
            DataFormatter formatter = new DataFormatter(new java.util.Locale("pt", "BR"));
            // Note: passing evaluator here might cause re-evaluation of formula. 
            // If we are here, formula evaluation might have been weird.
            String strVal = formatter.formatCellValue(cell, evaluator); 
            return parseStringValue(strVal);
            
        } catch (Exception e) {
            // Final safety net: try to get numeric value directly if possible
            if (cell.getCellType() == CellType.NUMERIC || 
               (cell.getCellType() == CellType.FORMULA && cell.getCachedFormulaResultType() == CellType.NUMERIC)) {
                return cell.getNumericCellValue();
            }
            logger.error("Error getting double value from cell", e);
            return 0.0;
        }
    }
    
    private Double parseStringValue(String val) {
         try {
             if (val == null || val.trim().isEmpty()) return 0.0;
             
             // Regex to keep only digits, minus sign and comma
             // This assumes Brazilian format where '.' is thousand separator and ',' is decimal
             val = val.replaceAll("[^0-9,-]", ""); 
             
             if (val.isEmpty() || val.equals("-")) return 0.0;
             
             // Replace decimal comma with dot for Java parsing
             val = val.replace(",", ".");
             
             return Double.parseDouble(val);
         } catch (NumberFormatException e) {
             logger.warn("Failed to parse string value: '{}'", val);
             return 0.0;
         }
    }

    private BigDecimal getBigDecimalValue(Cell cell, FormulaEvaluator evaluator) {
        Double val = getDoubleValue(cell, evaluator);
        return val != null ? BigDecimal.valueOf(val) : BigDecimal.ZERO;
    }
    
    private BigDecimal getBigDecimalValue(Cell cell) {
         // Helper for compatibility if needed, but we should use the one with evaluator
         return getBigDecimalValue(cell, null); // Will crash if not updated everywhere? No, handled above.
    }

    private java.time.LocalDate getDateValue(Cell cell) {
        if (cell == null) return null;
        try {
            if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                return cell.getDateCellValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            }
        } catch (Exception e) {
            return null;
        }
        return null; // fallback or parse string if needed
    }

    public byte[] exportVendasToExcel(List<Venda> vendas) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            
            // Formato brasileiro para datas
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            
            // ==================== ABA 1: VENDAS (CONSOLIDADO) ====================
            Sheet sheetVendas = workbook.createSheet("Vendas");
            createVendasSheet(sheetVendas, vendas, dateFormatter, workbook);
            
            // ==================== ABA 2: PRODUTOS (DETALHADO) ====================
            Sheet sheetProdutos = workbook.createSheet("Produtos Detalhados");
            createProdutosSheet(sheetProdutos, vendas, dateFormatter, workbook);
            
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Erro ao gerar arquivo Excel: " + e.getMessage());
        }
    }
    
    private void createVendasSheet(Sheet sheet, List<Venda> vendas, DateTimeFormatter dateFormatter, Workbook workbook) {
        // Criar estilos
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle currencyStyle = createCurrencyStyle(workbook);
        CellStyle normalStyle = createNormalStyle(workbook);
        
        // Cabeçalho
        Row headerRow = sheet.createRow(0);
        String[] columns = {"CLIENTE", "NF", "OV", "ENTREGA", "TELEFONE", "CIDADE", "ESTADO", 
                            "VENDEDOR", "DATA", "PLACAS", "INVERSOR", "POTÊNCIA", 
                            "R$ VENDA", "R$ MATERIAL", "R$ BRUTO", "MARKUP", "TIME"};
        
        for (int i = 0; i < columns.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(columns[i]);
            cell.setCellStyle(headerStyle);
        }
        
        // Dados
        int rowNum = 1;
        for (Venda venda : vendas) {
            Row row = sheet.createRow(rowNum++);
            
            createCell(row, 0, venda.getCliente(), normalStyle);
            createCell(row, 1, venda.getNf(), normalStyle);
            createCell(row, 2, venda.getOv(), normalStyle);
            createCell(row, 3, venda.getEntrega(), normalStyle);
            createCell(row, 4, venda.getTelefone(), normalStyle);
            createCell(row, 5, venda.getCidade(), normalStyle);
            createCell(row, 6, venda.getEstado(), normalStyle);
            createCell(row, 7, venda.getVendedor(), normalStyle);
            
            // DATA (formato brasileiro)
            Cell cell8 = row.createCell(8);
            if (venda.getData() != null) {
                cell8.setCellValue(venda.getData().format(dateFormatter));
            }
            cell8.setCellStyle(normalStyle);
            
            createCell(row, 9, venda.getPlacas(), normalStyle);
            createCell(row, 10, venda.getInversor(), normalStyle);
            createCell(row, 11, venda.getPotencia(), normalStyle);
            
            createCurrencyCell(row, 12, venda.getValorVenda(), workbook);
            createCurrencyCell(row, 13, venda.getValorMaterial(), workbook);
            createCurrencyCell(row, 14, venda.getValorBruto(), workbook);
            
            Cell cell15 = row.createCell(15);
            if (venda.getMarkup() != null) {
                cell15.setCellValue(venda.getMarkup().doubleValue() + "%");
            }
            cell15.setCellStyle(normalStyle);
            
            createCell(row, 16, venda.getTime(), normalStyle);
        }
        
        // Ajustar largura das colunas
        for (int i = 0; i < columns.length; i++) {
            sheet.autoSizeColumn(i);
            sheet.setColumnWidth(i, sheet.getColumnWidth(i) + 512);
        }
    }
    
    private void createProdutosSheet(Sheet sheet, List<Venda> vendas, DateTimeFormatter dateFormatter, Workbook workbook) {
        // Criar estilos
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle currencyStyle = createCurrencyStyle(workbook);
        CellStyle normalStyle = createNormalStyle(workbook);
        
        // Cabeçalho
        Row headerRow = sheet.createRow(0);
        String[] columns = {"ID VENDA", "CLIENTE", "NF", "OV", "CIDADE", "ESTADO", 
                            "VENDEDOR", "DATA", "TIME",
                            "PRODUTO", "GRUPO", "QUANTIDADE", 
                            "R$ UNITÁRIO VENDA", "R$ UNITÁRIO CUSTO", "R$ TOTAL PRODUTO", "R$ LUCRO PRODUTO"};
        
        for (int i = 0; i < columns.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(columns[i]);
            cell.setCellStyle(headerStyle);
        }
        
        // Dados - uma linha para cada produto
        int rowNum = 1;
        for (Venda venda : vendas) {
            if (venda.getProduto() != null && !venda.getProduto().isEmpty()) {
                for (VendaItem item : venda.getProduto()) {
                    Row row = sheet.createRow(rowNum++);
                    
                    // Dados da venda
                    Cell cellId = row.createCell(0);
                    if (venda.getId() != null) {
                        cellId.setCellValue(venda.getId());
                    }
                    cellId.setCellStyle(normalStyle);
                    
                    createCell(row, 1, venda.getCliente(), normalStyle);
                    createCell(row, 2, venda.getNf(), normalStyle);
                    createCell(row, 3, venda.getOv(), normalStyle);
                    createCell(row, 4, venda.getCidade(), normalStyle);
                    createCell(row, 5, venda.getEstado(), normalStyle);
                    createCell(row, 6, venda.getVendedor(), normalStyle);
                    
                    // DATA (formato brasileiro)
                    Cell cell7 = row.createCell(7);
                    if (venda.getData() != null) {
                        cell7.setCellValue(venda.getData().format(dateFormatter));
                    }
                    cell7.setCellStyle(normalStyle);
                    
                    createCell(row, 8, venda.getTime(), normalStyle);
                    
                    // Dados do produto
                    createCell(row, 9, item.getNomeProduto(), normalStyle);
                    createCell(row, 10, item.getGrupo(), normalStyle);
                    
                    Cell cellQtd = row.createCell(11);
                    if (item.getQuantidade() != null) {
                        cellQtd.setCellValue(item.getQuantidade());
                    }
                    cellQtd.setCellStyle(normalStyle);
                    
                    createCurrencyCell(row, 12, item.getValorUnitarioVenda(), workbook);
                    createCurrencyCell(row, 13, item.getValorUnitarioCusto(), workbook);
                    
                    // R$ TOTAL PRODUTO = quantidade * valor unitário venda
                    if (item.getQuantidade() != null && item.getValorUnitarioVenda() != null) {
                        BigDecimal totalProduto = item.getValorUnitarioVenda()
                            .multiply(BigDecimal.valueOf(item.getQuantidade()));
                        createCurrencyCell(row, 14, totalProduto, workbook);
                    } else {
                        createCurrencyCell(row, 14, null, workbook);
                    }
                    
                    // R$ LUCRO PRODUTO = (valor venda - valor custo) * quantidade
                    if (item.getQuantidade() != null && 
                        item.getValorUnitarioVenda() != null && 
                        item.getValorUnitarioCusto() != null) {
                        BigDecimal lucroProduto = item.getValorUnitarioVenda()
                            .subtract(item.getValorUnitarioCusto())
                            .multiply(BigDecimal.valueOf(item.getQuantidade()));
                        createCurrencyCell(row, 15, lucroProduto, workbook);
                    } else {
                        createCurrencyCell(row, 15, null, workbook);
                    }
                }
            }
        }
        
        // Ajustar largura das colunas
        for (int i = 0; i < columns.length; i++) {
            sheet.autoSizeColumn(i);
            sheet.setColumnWidth(i, sheet.getColumnWidth(i) + 512);
        }
    }
    
    // Métodos auxiliares para criar estilos e células
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.WHITE.getIndex());
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);
        return headerStyle;
    }
    
    private CellStyle createCurrencyStyle(Workbook workbook) {
        CellStyle currencyStyle = workbook.createCellStyle();
        DataFormat format = workbook.createDataFormat();
        currencyStyle.setDataFormat(format.getFormat("R$ #,##0.00"));
        return currencyStyle;
    }
    
    private CellStyle createNormalStyle(Workbook workbook) {
        CellStyle normalStyle = workbook.createCellStyle();
        normalStyle.setBorderBottom(BorderStyle.THIN);
        normalStyle.setBorderTop(BorderStyle.THIN);
        normalStyle.setBorderLeft(BorderStyle.THIN);
        normalStyle.setBorderRight(BorderStyle.THIN);
        return normalStyle;
    }
    
    private void createCell(Row row, int columnIndex, String value, CellStyle style) {
        Cell cell = row.createCell(columnIndex);
        cell.setCellValue(value != null ? value : "");
        cell.setCellStyle(style);
    }
    
    private void createCurrencyCell(Row row, int columnIndex, BigDecimal value, Workbook workbook) {
        Cell cell = row.createCell(columnIndex);
        if (value != null) {
            cell.setCellValue(value.doubleValue());
            CellStyle currencyWithBorder = workbook.createCellStyle();
            currencyWithBorder.cloneStyleFrom(createCurrencyStyle(workbook));
            currencyWithBorder.setBorderBottom(BorderStyle.THIN);
            currencyWithBorder.setBorderTop(BorderStyle.THIN);
            currencyWithBorder.setBorderLeft(BorderStyle.THIN);
            currencyWithBorder.setBorderRight(BorderStyle.THIN);
            cell.setCellStyle(currencyWithBorder);
        } else {
            CellStyle normalStyle = createNormalStyle(workbook);
            cell.setCellStyle(normalStyle);
        }
    }
}
