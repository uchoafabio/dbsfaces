package br.com.dbsoft.ui.core;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Exporta os dados de origem, sem utilizar o layout ou a paginação do Jasper.
 * Listas filhas geram linhas com os dados do pai repetidos e colunas prefixadas
 * pelo caminho (ex.: dados_ativo.produto). Listas irmãs são blocos independentes,
 * nunca um produto cartesiano. Cálculos exclusivos do Jasper precisam ser
 * fornecidos pelo bean em getCSVData().
 */
public final class DBSReportCSV {

	private DBSReportCSV() {}

	/**
	 * @param pColumns Caminho do campo -> título, na ordem desejada. Null exporta
	 * todos os campos em ordem alfabética, inclusive campos ausentes na primeira linha.
	 * @return CSV UTF-8 com BOM, separador ponto e vírgula e linhas CRLF.
	 */
	public static byte[] create(Collection<?> pData, Map<String, String> pColumns) throws IOException {
		List<Map<String, Object>> xRows = new ArrayList<Map<String, Object>>();
		Set<Object> xAncestors = Collections.newSetFromMap(new IdentityHashMap<Object, Boolean>());
		if (pData != null) {
			for (Object xRecord : pData) {
				flatten(xRecord, "", Collections.<String, Object>emptyMap(), xAncestors, xRows);
			}
		}
		Map<String, String> xColumns = new LinkedHashMap<String, String>();
		if (pColumns != null) {
			xColumns.putAll(pColumns);
		} else {
			Set<String> xNames = new TreeSet<String>();
			for (Map<String, Object> xRow : xRows) {
				xNames.addAll(xRow.keySet());
			}
			for (String xName : xNames) {
				xColumns.put(xName, xName);
			}
		}
		ByteArrayOutputStream xOutput = new ByteArrayOutputStream();
		Writer xWriter = new OutputStreamWriter(xOutput, "UTF-8");
		xWriter.write('\uFEFF');
		if (!xColumns.isEmpty()) {
			writeRow(xWriter, new ArrayList<Object>(xColumns.values()));
			for (Map<String, Object> xRow : xRows) {
				List<Object> xValues = new ArrayList<Object>();
				for (String xName : xColumns.keySet()) {
					xValues.add(xRow.get(xName));
				}
				writeRow(xWriter, xValues);
			}
		}
		xWriter.flush();
		return xOutput.toByteArray();
	}

	private static void flatten(Object pRecord, String pPath, Map<String, Object> pParent,
			Set<Object> pAncestors, List<Map<String, Object>> pRows) throws IOException {
		Map<String, Object> xValues = new LinkedHashMap<String, Object>(pParent);
		Map<String, Collection<?>> xChildren = new LinkedHashMap<String, Collection<?>>();
		collect(pRecord, pPath, pAncestors, xValues, xChildren);
		boolean xHasChildren = false;
		for (Collection<?> xChild : xChildren.values()) {
			xHasChildren |= !xChild.isEmpty();
		}
		if (!xHasChildren) {
			pRows.add(xValues);
			return;
		}
		enter(pRecord, pAncestors);
		try {
			for (Map.Entry<String, Collection<?>> xChild : xChildren.entrySet()) {
				enter(xChild.getValue(), pAncestors);
				try {
					for (Object xItem : xChild.getValue()) {
						flatten(xItem, xChild.getKey(), xValues, pAncestors, pRows);
					}
				} finally {
					pAncestors.remove(xChild.getValue());
				}
			}
		} finally {
			pAncestors.remove(pRecord);
		}
	}

	private static void collect(Object pValue, String pPath, Set<Object> pAncestors,
			Map<String, Object> pValues, Map<String, Collection<?>> pChildren) throws IOException {
		if (pValue == null || pValue instanceof CharSequence || pValue instanceof Number
				|| pValue instanceof Boolean || pValue instanceof Character
				|| pValue instanceof Date || pValue instanceof Enum<?>) {
			pValues.put(pPath.length() == 0 ? "valor" : pPath, pValue);
			return;
		}
		if (pValue instanceof Collection<?>) {
			pChildren.put(pPath, (Collection<?>) pValue);
			return;
		}
		enter(pValue, pAncestors);
		try {
			Map<String, Object> xProperties = properties(pValue);
			for (Map.Entry<String, Object> xProperty : xProperties.entrySet()) {
				String xPath = pPath.length() == 0 ? xProperty.getKey() : pPath + "." + xProperty.getKey();
				collect(xProperty.getValue(), xPath, pAncestors, pValues, pChildren);
			}
		} finally {
			pAncestors.remove(pValue);
		}
	}

	private static void enter(Object pValue, Set<Object> pAncestors) throws IOException {
		if (!pAncestors.add(pValue)) {
			throw new IOException("Referência circular nos dados do CSV.");
		}
	}

	private static Map<String, Object> properties(Object pValue) throws IOException {
		Map<String, Object> xProperties = new TreeMap<String, Object>();
		if (pValue instanceof Map<?, ?>) {
			for (Map.Entry<?, ?> xEntry : ((Map<?, ?>) pValue).entrySet()) {
				if (!(xEntry.getKey() instanceof String)) {
					throw new IOException("As colunas do CSV devem possuir nomes textuais.");
				}
				xProperties.put((String) xEntry.getKey(), xEntry.getValue());
			}
			return xProperties;
		}
		try {
			for (PropertyDescriptor xProperty : Introspector.getBeanInfo(pValue.getClass(), Object.class).getPropertyDescriptors()) {
				if (xProperty.getReadMethod() != null) {
					xProperties.put(xProperty.getName(), xProperty.getReadMethod().invoke(pValue));
				}
			}
		} catch (IntrospectionException e) {
			throw new IOException("Erro ao ler as colunas do CSV.", e);
		} catch (IllegalAccessException e) {
			throw new IOException("Erro ao acessar os dados do CSV.", e);
		} catch (InvocationTargetException e) {
			throw new IOException("Erro ao recuperar os dados do CSV.", e.getCause());
		}
		if (xProperties.isEmpty()) {
			throw new IOException("Tipo não suportado no CSV: " + pValue.getClass().getName());
		}
		return xProperties;
	}

	private static void writeRow(Writer pWriter, List<Object> pValues) throws IOException {
		boolean xFirst = true;
		for (Object xValue : pValues) {
			if (!xFirst) {
				pWriter.write(';');
			}
			xFirst = false;
			String xText = text(xValue);
			// Aspas CSV não impedem que o Excel execute fórmulas recebidas como texto.
			if (xValue instanceof CharSequence && isFormula(xText)) {
				xText = "'" + xText;
			}
			pWriter.write('"');
			pWriter.write(xText.replace("\"", "\"\""));
			pWriter.write('"');
		}
		pWriter.write("\r\n");
	}

	private static boolean isFormula(String pText) {
		String xText = pText.trim();
		// Muitos relatórios já entregam números como String. Preserve-os para
		// permitir somas e filtros numéricos no Excel.
		if (xText.matches("[+-]?(?:[0-9]+(?:[.,][0-9]*)?|[.,][0-9]+)(?:[eE][+-]?[0-9]+)?")) {
			return false;
		}
		return xText.startsWith("=") || xText.startsWith("+") || xText.startsWith("-")
				|| xText.startsWith("@") || pText.startsWith("\t")
				|| pText.startsWith("\r") || pText.startsWith("\n");
	}

	private static String text(Object pValue) {
		if (pValue == null) {
			return "";
		}
		if (pValue instanceof java.sql.Time) {
			return new SimpleDateFormat("HH:mm:ss").format(pValue);
		}
		if (pValue instanceof Date) {
			return new SimpleDateFormat(pValue instanceof java.sql.Date ? "dd/MM/yyyy" : "dd/MM/yyyy HH:mm:ss").format(pValue);
		}
		if (pValue instanceof BigDecimal) {
			return ((BigDecimal) pValue).toPlainString().replace('.', ',');
		}
		if (pValue instanceof Number) {
			return pValue.toString().replace('.', ',');
		}
		return pValue.toString();
	}
}
