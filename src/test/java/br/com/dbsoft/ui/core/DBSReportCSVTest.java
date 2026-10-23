package br.com.dbsoft.ui.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;

public class DBSReportCSVTest {

	@Test
	public void writesExcelCompatibleCSVWithoutLosingPrecisionOrAccents() throws Exception {
		Map<String, Object> xRow = row("cliente", "São João; \"Matriz\"\nFilial", "valor", new BigDecimal("1234567890.123456789"),
				"data", java.sql.Date.valueOf("2026-10-23"), "vazio", null, "negativo", -12.5);
		byte[] xBytes = DBSReportCSV.create(Collections.singletonList(xRow), null);
		assertEquals(0xEF, xBytes[0] & 0xFF);
		assertEquals(0xBB, xBytes[1] & 0xFF);
		assertEquals(0xBF, xBytes[2] & 0xFF);
		assertEquals("\uFEFF\"cliente\";\"data\";\"negativo\";\"valor\";\"vazio\"\r\n"
				+ "\"São João; \"\"Matriz\"\"\nFilial\";\"23/10/2026\";\"-12,5\";\"1234567890,123456789\";\"\"\r\n", new String(xBytes, "UTF-8"));
	}

	@Test
	public void includesFieldsFoundOnlyInLaterRecordsAndWritesOneHeader() throws Exception {
		assertEquals("\uFEFF\"a\";\"b\"\r\n\"1\";\"\"\r\n\"\";\"2\"\r\n",
				csv(row("a", 1), row("b", 2)));
	}

	@Test
	public void repeatsParentValuesForEachNestedDetail() throws Exception {
		Map<String, Object> xRow = row("carteira", "A", "dados", Arrays.asList(
				row("ativo", "X", "quantidade", 10), row("ativo", "Y", "quantidade", 20)));
		assertEquals("\uFEFF\"carteira\";\"dados.ativo\";\"dados.quantidade\"\r\n"
				+ "\"A\";\"X\";\"10\"\r\n\"A\";\"Y\";\"20\"\r\n", csv(xRow));
	}

	@Test
	public void keepsSiblingSubreportsIndependentInsteadOfMultiplyingRecords() throws Exception {
		Map<String, Object> xRow = row("fundo", "A", "ativos", Arrays.asList(row("nome", "X"), row("nome", "Y")),
				"setores", Arrays.asList(row("nome", "Financeiro"), row("nome", "Indústria")));
		assertEquals("\uFEFF\"ativos.nome\";\"fundo\";\"setores.nome\"\r\n"
				+ "\"X\";\"A\";\"\"\r\n\"Y\";\"A\";\"\"\r\n"
				+ "\"\";\"A\";\"Financeiro\"\r\n\"\";\"A\";\"Indústria\"\r\n", csv(xRow));
	}

	@Test
	public void flattensMultipleLevelsAndPreservesParentsWithEmptyDetails() throws Exception {
		Map<String, Object> xRow = row("cliente", "A", "grupos", Arrays.asList(
				row("nome", "G1", "itens", Collections.singletonList(row("valor", 2))),
				row("nome", "G2", "itens", Collections.emptyList())));
		assertEquals("\uFEFF\"cliente\";\"grupos.itens.valor\";\"grupos.nome\"\r\n"
				+ "\"A\";\"2\";\"G1\"\r\n\"A\";\"\";\"G2\"\r\n", csv(xRow));
	}

	@Test
	public void supportsExplicitColumnNamesOrderAndSelection() throws Exception {
		Map<String, String> xColumns = new LinkedHashMap<String, String>();
		xColumns.put("valor", "Valor total");
		xColumns.put("cliente", "Cliente");
		assertEquals("\uFEFF\"Valor total\";\"Cliente\"\r\n\"10\";\"A\"\r\n",
				new String(DBSReportCSV.create(Collections.singletonList(row("cliente", "A", "valor", 10, "interno", 123)), xColumns), "UTF-8"));
		assertEquals("\uFEFF\"Valor total\";\"Cliente\"\r\n",
				new String(DBSReportCSV.create(Collections.emptyList(), xColumns), "UTF-8"));
	}

	@Test
	public void readsBeansAndNestedMaps() throws Exception {
		assertEquals("\uFEFF\"cliente.nome\";\"valor\"\r\n\"A\";\"10\"\r\n",
				new String(DBSReportCSV.create(Collections.singletonList(new Record()), null), "UTF-8"));
	}

	@Test
	public void neutralizesTextFormulasAndKeepsNegativeNumbersNumeric() throws Exception {
		assertEquals("\uFEFF\"a\";\"b\";\"c\";\"d\";\"e\";\"f\"\r\n"
				+ "\"'=1+1\";\"'+1+1\";\"'-1+1\";\"' @SUM(A1)\";\"'\tX\";\"-1\"\r\n",
				csv(row("a", "=1+1", "b", "+1+1", "c", "-1+1", "d", " @SUM(A1)", "e", "\tX", "f", -1)));
	}

	@Test
	public void preservesNumbersProvidedAsTextForExcelArithmetic() throws Exception {
		assertEquals("\uFEFF\"a\";\"b\";\"c\"\r\n\"-123,45\";\"+12.5\";\"-1,25E-7\"\r\n",
				csv(row("a", "-123,45", "b", "+12.5", "c", "-1,25E-7")));
	}

	@Test(expected = IOException.class)
	public void rejectsCircularMaps() throws Exception {
		Map<String, Object> xRow = row("cliente", "A");
		xRow.put("pai", xRow);
		csv(xRow);
	}

	@Test(expected = IOException.class)
	public void rejectsCircularSubreportLists() throws Exception {
		Map<String, Object> xRow = row("cliente", "A");
		xRow.put("filhos", Collections.singletonList(xRow));
		csv(xRow);
	}

	@Test
	public void allowsTheSameRecordInDifferentBranches() throws Exception {
		Map<String, Object> xDetail = row("nome", "X");
		String xCSV = csv(row("a", Collections.singletonList(xDetail), "b", Collections.singletonList(xDetail)));
		assertTrue(xCSV.contains("\"X\";\"\"\r\n\"\";\"X\"\r\n"));
	}

	@Test
	public void supportsEmptyData() throws Exception {
		assertEquals("\uFEFF", new String(DBSReportCSV.create(Collections.emptyList(), null), "UTF-8"));
	}

	public static class Record {
		public Map<String, Object> getCliente() { return row("nome", "A"); }
		public int getValor() { return 10; }
	}

	@SafeVarargs
	private static String csv(Map<String, Object>... pRows) throws Exception {
		return new String(DBSReportCSV.create(Arrays.asList(pRows), null), "UTF-8");
	}

	private static Map<String, Object> row(Object... pPairs) {
		Map<String, Object> xRow = new LinkedHashMap<String, Object>();
		for (int xIndex = 0; xIndex < pPairs.length; xIndex += 2) {
			xRow.put((String) pPairs[xIndex], pPairs[xIndex + 1]);
		}
		return xRow;
	}
}
