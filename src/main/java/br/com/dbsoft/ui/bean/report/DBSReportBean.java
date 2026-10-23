package br.com.dbsoft.ui.bean.report;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import br.com.dbsoft.error.DBSIOException;
import br.com.dbsoft.message.IDBSMessageBase.MESSAGE_TYPE;
import br.com.dbsoft.ui.bean.DBSBeanModalMessages;
import br.com.dbsoft.ui.bean.report.DBSReportBeanEvent.REPORT_EVENT;
import br.com.dbsoft.ui.core.DBSReportCSV;
import br.com.dbsoft.ui.core.DBSReportFormUtil;
import br.com.dbsoft.util.DBSFormat;
import br.com.dbsoft.util.DBSNumber;
import br.com.dbsoft.util.DBSObject;
import br.com.dbsoft.util.DBSString;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

/**
 * Os DBSReportBean devem ser declarados como @ConversationScoped.
 * O timeout da conversação esta definido em 10minutos
 * @author ricardo.villar
 *
 */
public abstract class DBSReportBean extends DBSBeanModalMessages {

	private static final long serialVersionUID = -4729336621811839199L;

	private static final String tabPageIdFiltros = "filtros";
	private static final String tabPageIdVisualizar = "visualizar";
	
	private List<IDBSReportBeanEventsListener>	wEventListeners = new ArrayList<IDBSReportBeanEventsListener>();
	private String					wPDFFilePath = "";
	private Integer					wRecordCount;
	private Boolean					wShowFilters = true;
	private JasperPrint 			wJasperPrint = null;
	private byte[] wCSVFile;
	private boolean wCSVFailed;
	private String					wCaption;
	private String wCurrentTabPage = tabPageIdFiltros;

	@Override
	protected void initializeClass() {
		//Inicia conversação
		conversationBegin();
	    pvFireEventInitialize();
//		DBSFaces.finalizeDBSBeans(this, false); << Comentado pois os beans passaram a ser criados como ConversationScoped - 12/Ago/2014
	}

	@Override
	protected void finalizeClass(){
		pvFireEventFinalize();
	}
	
	public String getCaption() {
		return wCaption;
	}
	
	/**
	 * Retorna constante com o nome do filtro.
	 * get necessário para que o bean consiga visualizar a constante.
	 * @return
	 */
	public String getTabPageIdFiltros(){
		return tabPageIdFiltros;
	}
	
	/**
	 * Retorna constante com o nome da aba de visualização.
	 * get necessário para que o bean consiga visualizar a constante.
	 * @return
	 */
	public String getTabPageIdVisualizar(){
		return tabPageIdVisualizar;
	}
	

	/**
	 * Texto que será exibido no cabeçalho do dialog
	 * @param pCaption
	 */
	public void setCaption(String pCaption) {
		wCaption = pCaption;
	}

	//---------------------------------Getters And Setters---------------------------------
	public String getPDFFilePath() {
		return wPDFFilePath;
	}
	
	public void setPDFFilePath(String pPDFFilePath) {
		wPDFFilePath = pPDFFilePath;
	}
	
	/**
	 * Retorna a quantidade de registros lidos
	 * @return
	 */
	public Integer getRecordCount() {
		return wRecordCount;
	}

	public void setRecordCount(Integer pRecordCount) {
		wRecordCount = pRecordCount;
	}

	/**
	 * Retorna se relatório já foi criado
	 * @return
	 */
	public Boolean getIsCreated() {
		if (wJasperPrint != null &&
			wJasperPrint.getPages().size() >0){
			return true;
		}
		return false;
	}
	
	
	public Boolean getShowFilters(){
		//Sempre exibe o filtro caso o relatório ainda não tenha sido criado(gerado)
		if (getIsCreated()){
			return wShowFilters;
		}else{
			return true;
		}
	}

	public void setShowFilters(Boolean pShowFilters){
		wShowFilters = pShowFilters;
	}


	/**
	 * Retorna a aba corrente
	 * @return
	 */
	public String getCurrentTabPage(){
		return wCurrentTabPage;
	}
	
	/**
	 * Ativa a aba dos filtros 
	 */
	public void showTabPageFiltros(){
		wCurrentTabPage = tabPageIdFiltros;
	}
	
	
	//---------------------------------Overrides----------------------------------------
	@Override
	public String setMessageValidated(Boolean pIsValidated) throws DBSIOException {
		//Salva o messagetype pos o superSetMessageValidated irá a mensagem corrente após a validação.
		MESSAGE_TYPE xMessageType = getMessageType();
		//Salva o nome da view para onde será direcionado. 
		String xView = super.setMessageValidated(pIsValidated);
		//Força o nova geração do relatório após a confirmação de uma mensagem de WARNING
		if (xMessageType.getIsQuestion() 
		 && pIsValidated){
			create();
		}
		return xView;
	}

	
	//---------------------------------Métodos Concretos---------------------------------
	
	/**
	 * Cria relatório.
	 */
	public synchronized void create() throws DBSIOException {
		wCSVFile = null;
		wCSVFailed = false;
		//Chama validação e NÃO gera relatório em caso de erro.
		if (!pvFireEventValidate()){
			//Mantem na aba de filtro
			wCurrentTabPage = tabPageIdFiltros;
			return;
		}
		
		//Ativa a aba de visualização
		wCurrentTabPage = tabPageIdVisualizar;

		//Apaga as mensagens para obrigar que as mensagens de warning incluidas no validades(se houverem), sejam recriadas
		clearMessages();

		Long xStartTime = System.currentTimeMillis();
		try {
			this.openConnection();
			//Recupera dos dados
			JRBeanCollectionDataSource xDados = getCollectionDataSource();
			//Tempo de processamento do relatório
			Long xTempo = System.currentTimeMillis() - xStartTime;
			wLogger.info("Dados recuperados em " + DBSFormat.getFormattedNumber(DBSNumber.divide(xTempo.doubleValue(), 1000D).doubleValue(), 3) + " segundos.");
			if (xDados == null) {
				wJasperPrint = DBSReportFormUtil.createJasperPrint(pvGetReportName(), getReportParameters(), wConnection);
				setPDFFilePath(DBSReportFormUtil.createPDFFile(pvGetReportName(), wJasperPrint));
				//Esconde seleção dos filtros
				setShowFilters(false);
			} else {
				wJasperPrint = DBSReportFormUtil.createJasperPrint(pvGetReportName(), getReportParameters(), xDados);
				if (DBSObject.isEmpty(wJasperPrint)) {
					addMessage("vazio", MESSAGE_TYPE.INFORMATION, "Relatório não encontrado.");
				}
				setPDFFilePath(DBSReportFormUtil.createPDFFile(pvGetReportName(), wJasperPrint));
				if (xDados.getRecordCount() > 0){
					//Esconde seleção dos filtros
					setShowFilters(false);
				}else{
					addMessage("vazio", MESSAGE_TYPE.INFORMATION, "Relatório sem informação.");
				}
			}
			// Guarda o CSV da mesma consulta do PDF, antes de fechar a conexão.
			// Falhas na exportação tabular não impedem a visualização do relatório.
			if (getIsCreated()) {
				try {
					Collection<?> xCSVData = getCSVData(xDados);
					if (xCSVData != null) {
						wCSVFile = DBSReportCSV.create(xCSVData, getCSVColumns());
					}
				} catch (Exception e) {
					wCSVFailed = true;
					wLogger.error("Erro ao preparar CSV do relatório " + pvGetReportName(), e);
				}
			}
			xTempo = System.currentTimeMillis() - xStartTime;
			wLogger.info("Relatório gerado em " +  DBSFormat.getFormattedNumber(DBSNumber.divide(xTempo.doubleValue(), 1000D).doubleValue(), 3) + " segundos.");
		} catch (DBSIOException e) {
			wLogger.error(e);
			addMessage("erro_escrita", MESSAGE_TYPE.ERROR, "Relatório com erro de escrita");
		} catch (JRException e) {
			wLogger.error(e);
			addMessage("erro_gerar", MESSAGE_TYPE.ERROR, "Erro ao gerar relatório");
		} catch (IOException e) {
			wLogger.error(e);
		} catch (Exception e) {
			wLogger.error(e);
			addMessage("erro_gerar", MESSAGE_TYPE.ERROR, "Erro ao gerar relatório");
		}finally{
			this.closeConnection();
		}
	}
	
	/**
	 * Inicia o Download do Relatório em PDF.
	 */
	public synchronized void savePDF() {
		//DBSReportFormUtil.savePDF(wPDFFilePath);
		DBSReportFormUtil.savePDF(pvGetReportName(), wJasperPrint);
	}
	
	/* 
	 * Inicia o Download do Relatório em XLS
	 */
	public synchronized void saveXLS() {
//		DBSReportFormUtil.saveXLS(pvGetReportName(), wJasperPrint);
		DBSReportFormUtil.saveXLSX(pvGetReportName(), wJasperPrint);
	}

	/** Retorna se há dados tabulares para o relatório gerado. */
	public Boolean getIsCSVAvailable() {
		return getIsCreated() && (wCSVFile != null || wCSVFailed);
	}

	/** Inicia o download dos dados da mesma geração exibida em PDF. */
	public synchronized void saveCSV() {
		if (!getIsCreated() || wCSVFile == null) {
			addMessage("erro_csv", MESSAGE_TYPE.ERROR, "Não foi possível gerar o CSV deste relatório.");
			return;
		}
		if (!DBSReportFormUtil.saveCSV(pvGetReportName(), wCSVFile)) {
			addMessage("erro_csv", MESSAGE_TYPE.ERROR, "Não foi possível baixar o CSV deste relatório.");
		}
	}

	/* 
	 * Inicia o Download do Relatório em XML
	 */
	public synchronized void saveXML() {
		DBSReportFormUtil.saveXML(pvGetReportName(), wJasperPrint);
	}

	/* 
	 * Inicia o Download do Relatório em HTML
	 */
	@Deprecated
	public synchronized void saveHTML() {
		DBSReportFormUtil.saveHTML(pvGetReportName(), wJasperPrint);
	}
	
	// PROTECTED ============================================================================
	/* ESTES MÉTODOS FORAM DEFINIDOS DIRETAMENTE NO CÓDIGO DESTA CLASS, AO INVÉS 
	* DE IMPLEMENTAR IDBSCrudEventsListeners, PARA QUE FIQUEM COMO PROTECTED. 
	* EVITANDO QUE ESTES MÉTODOS SEJAM CHAMADOS EXTERNAMENTE POR OUTRAS CLASSES.
	* O QUE SERIA UM PROBLEMA DE SEGURANÇA JÁ QUE ESTA CLASS(DBSReposrtBean) SERÁ UTILIZADA COMO UM MANAGEDBEAN.
	*/
	
	/**
	 * Evento chamado logo na inicialização do bean.<br/>
	 * Evento utlizado normalmente para configurar os valores iniciais dos filtros e título do relatório.
	 * @param pEvent
	 * @throws DBSIOException
	 */
	protected abstract void initialize(DBSReportBeanEvent pEvent) throws DBSIOException;

	/**
	 * Chamado quando a class é finalizada.
	 * @param pEvent Informações do evento
	 */
	protected void finalize(DBSReportBeanEvent pEvent){};
	
	/**
	 * Evento chamado antes de iniciar a criação do relatório.<br/>
	 * Para indicar problemas na validação deve-se setar <b>pEvent.setOk(false)</b>.
	 * Neste método devesse efetuar as validações de regras de negócios e gerar as mensagens de erro ou alerta, 
	 * caso necessario, via o comando addMessage.<br/>
	 * @param pEvent Informações do evento
	 * @throws DBSIOException
	 */
	protected void validate(DBSReportBeanEvent pEvent) throws DBSIOException{};
	

	/**
	 * Método para definição de Parametros.<br/>
	 * A conexão já se encontra aberta. 
	 * @return Map<String, Object>
	 */
	protected abstract Map<String, Object> getReportParameters() throws DBSIOException;
	
	/**
	 * Método para recuperar os dados a serem apresentados no relatório.<br/>
	 * A conexão já se encontra aberta.
	 * @return JRBeanCollectionDataSource
	 */
	protected abstract JRBeanCollectionDataSource getCollectionDataSource() throws DBSIOException;
	
	/**
	 * Dados tabulares da mesma consulta utilizada pelo Jasper. Sobrescreva para
	 * incluir cálculos do layout ou adaptar sub-relatórios. Para relatórios SQL,
	 * pData é null e o padrão não oferece CSV; a conexão permanece aberta aqui.
	 */
	protected Collection<?> getCSVData(JRBeanCollectionDataSource pData) throws DBSIOException {
		return pData == null ? null : pData.getData();
	}

	/**
	 * Caminho do campo -> título da coluna. Use LinkedHashMap para definir ordem.
	 * Ex.: dados_ativo.produto -> Produto. Null inclui todos os campos de origem
	 * em ordem alfabética. Cálculos do Jasper precisam ser incluídos em getCSVData.
	 */
	protected Map<String, String> getCSVColumns() {
		return null;
	}

	
	// Private ==============================================================================
	/**
	 * Retorna o nome do arquivo do relatório.
	 * O nome será o mesmo nome do bean sem a paralavra 'bean' se houver.
	 * 
	 * @return String Nome do Arquivo Relatório.
	 */
	private String pvGetReportName(){
		String xReportName = this.getClass().getSimpleName();
		xReportName = DBSString.changeStr(xReportName, "Bean", "", false);
		xReportName = xReportName.substring(0, 1).toLowerCase() + xReportName.substring(1, xReportName.length());
		return xReportName; 
	}

	//Privates------------------------------------------------------------------------------------
	private void pvFireEventInitialize(){
		DBSReportBeanEvent xE = new DBSReportBeanEvent(this, REPORT_EVENT.INITIALIZE);
		try {
			pvBroadcastEvent(xE, true, true);
		} catch (Exception e) {
			xE.setOk(false);
			wLogger.error("EventInitialize",e);
		}
	}	

	private void pvFireEventFinalize(){
		DBSReportBeanEvent xE = new DBSReportBeanEvent(this, REPORT_EVENT.FINALIZE);
		try {
			pvBroadcastEvent(xE, false, true);
		} catch (Exception e) {
			xE.setOk(false);
			wLogger.error("EventInitialize",e);
		}
	}	
	
	/**
	 * Antes das criação do relatório 
	 * @return
	 */
	private boolean pvFireEventValidate(){
		DBSReportBeanEvent xE = new DBSReportBeanEvent(this, REPORT_EVENT.VALIDATE);
		try {
			pvBroadcastEvent(xE, false, true);
		} catch (Exception e) {
			xE.setOk(false);
			wLogger.error("EventInitialize",e);
		}
		return xE.isOk();
	}

	
	/**
	 * Chama os eventos localmente, nos filhos e nos listerners que eventualmente possam existir.
	 * @param pEvent
	 * @param pOpenConnection Se abre a conexão
	 * @param pCloseConnection Se fecha a conexão
	 * @throws Exception 
	 */
	private void pvBroadcastEvent(DBSReportBeanEvent pEvent, boolean pOpenConnection, boolean pCloseConnection) throws Exception {
		try{
			if (pOpenConnection){
				openConnection();
			}
			wBrodcastingEvent = true;
			pvFireEventLocal(pEvent);
			pvFireEventListeners(pEvent);
		}catch(DBSIOException e){
			wMessageError.setMessageText(e.getLocalizedMessage()); 
			addMessage(wMessageError);
			pEvent.setOk(false);
		}catch(Exception e){
			String xStr = pEvent.getEvent().toString();   
			if (e.getLocalizedMessage()!=null){
				 xStr = xStr + e.getLocalizedMessage();
			}else{
				xStr = xStr + e.getClass();
			}
			wMessageError.setMessageTextParameters(xStr);
			addMessage(wMessageError);
			pEvent.setOk(false);
			throw e;
		}finally{
			wBrodcastingEvent = false;
			if (pCloseConnection || !pEvent.isOk()){
				closeConnection();
			}
		}	
	}
	
	//Chama a metodo(evento) dentro das classe foram adicionadas na lista que possuem a implementação da respectiva interface
	private void pvFireEventLocal(DBSReportBeanEvent pEvent) throws DBSIOException{
		if (pEvent.isOk()){
			switch (pEvent.getEvent()) {
			case INITIALIZE:
				initialize(pEvent);
				break;
			case FINALIZE:
				finalize(pEvent);
				break;
			case VALIDATE:
				validate(pEvent);
				break;
			default:
				break;
			}
		}		
	}
	
	//Chama a metodo(evento) dentro das classe foram adicionadas na lista que possuem a implementação da respectiva interface
	private void pvFireEventListeners(DBSReportBeanEvent pEvent){
		if (pEvent.isOk()){
			for (int xX=0; xX<wEventListeners.size(); xX++){
				switch (pEvent.getEvent()) {
				case INITIALIZE:
					wEventListeners.get(xX).initialize(pEvent);
					break;
				case FINALIZE:
					wEventListeners.get(xX).finalize(pEvent);
					break;
				case VALIDATE:
					wEventListeners.get(xX).validate(pEvent);
					break;
				default:
					break;
				}
				//Sa do loop se encontrar erro
				if (!pEvent.isOk()){
					break;
				}
	        }
		}		
	}
	

}
