package net.canadensys.harvester.config;

import java.beans.PropertyVetoException;
import java.util.Properties;

import javax.sql.DataSource;

import net.canadensys.dataportal.occurrence.model.OccurrenceModel;
import net.canadensys.dataportal.occurrence.model.OccurrenceRawModel;
import net.canadensys.dataportal.occurrence.model.ResourceContactModel;
import net.canadensys.harvester.ItemProcessorIF;
import net.canadensys.harvester.ItemReaderIF;
import net.canadensys.harvester.ItemTaskIF;
import net.canadensys.harvester.ItemWriterIF;
import net.canadensys.harvester.ProcessingStepIF;
import net.canadensys.harvester.config.harvester.HarvesterConfigIF;
import net.canadensys.harvester.jms.JMSConsumer;
import net.canadensys.harvester.jms.JMSWriter;
import net.canadensys.harvester.occurrence.job.ComputeUniqueValueJob;
import net.canadensys.harvester.occurrence.job.ImportDwcaJob;
import net.canadensys.harvester.occurrence.job.MoveToPublicSchemaJob;
import net.canadensys.harvester.occurrence.model.ImportLogModel;
import net.canadensys.harvester.occurrence.model.ResourceModel;
import net.canadensys.harvester.occurrence.processor.DwcaLineProcessor;
import net.canadensys.harvester.occurrence.processor.OccurrenceProcessor;
import net.canadensys.harvester.occurrence.processor.ResourceContactProcessor;
import net.canadensys.harvester.occurrence.reader.DwcaItemReader;
import net.canadensys.harvester.occurrence.step.InsertRawOccurrenceStep;
import net.canadensys.harvester.occurrence.step.InsertResourceContactStep;
import net.canadensys.harvester.occurrence.step.ProcessInsertOccurrenceStep;
import net.canadensys.harvester.occurrence.view.model.HarvesterViewModel;
import net.canadensys.harvester.occurrence.writer.OccurrenceHibernateWriter;
import net.canadensys.harvester.occurrence.writer.RawOccurrenceHibernateWriter;
import net.canadensys.harvester.occurrence.writer.ResourceContactHibernateWriter;

import org.gbif.metadata.eml.Eml;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.core.io.FileSystemResource;
import org.springframework.orm.hibernate4.HibernateTransactionManager;
import org.springframework.orm.hibernate4.LocalSessionFactoryBean;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.mchange.v2.c3p0.ComboPooledDataSource;

/**
 * Configuration class using Spring annotations.
 * All the beans are created from here.
 * @author canadensys
 *
 */
@Configuration
@ComponentScan(basePackages ="net.canadensys.harvester",
	excludeFilters = { @Filter(type = FilterType.CUSTOM, value = { ExcludeTestClassesTypeFilter.class }),
		@Filter(type = FilterType.ASSIGNABLE_TYPE, value = { ProcessingConfig.class })})
@EnableTransactionManagement
public class ProcessingNodeConfig {
	
    @Bean
    public static PropertyPlaceholderConfigurer properties(){
    	PropertyPlaceholderConfigurer ppc = new PropertyPlaceholderConfigurer();
    	ppc.setLocation( new FileSystemResource("config/harvester-config.properties") );
    	return ppc;
    }
    
    @Value("${database.url}")
    private String dbUrl;
    @Value( "${database.driver}" )
    private String dbDriverClassName;
    @Value( "${database.username}" )
    private String username;
    @Value( "${database.password}" )
    private String password;
    
    @Value( "${hibernate.dialect}" )
    private String hibernateDialect;
    @Value( "${hibernate.show_sql}" )
    private String hibernateShowSql;
    @Value( "${hibernate.buffer_schema}" )
    private String hibernateBufferSchema;
    
    @Value("${jms.broker_url}")
    private String jmsBrokerUrl;
    
    @Bean(name="datasource")
    public DataSource dataSource() {    		
    	ComboPooledDataSource ds = new ComboPooledDataSource();
    	try {
			ds.setDriverClass(dbDriverClassName);
		} catch (PropertyVetoException e) {
			e.printStackTrace();
		}
    	ds.setJdbcUrl(dbUrl);
    	ds.setUser(username);
    	ds.setPassword(password);
    	return ds;
    }
    
    @Bean(name="bufferSessionFactory")
    public LocalSessionFactoryBean bufferSessionFactory() {
    	LocalSessionFactoryBean sb = new LocalSessionFactoryBean(); 
    	sb.setDataSource(dataSource()); 
    	sb.setAnnotatedClasses(new Class[]{OccurrenceRawModel.class,
    			OccurrenceModel.class,ImportLogModel.class,ResourceContactModel.class});

		Properties hibernateProperties = new Properties();
		hibernateProperties.setProperty("hibernate.dialect", hibernateDialect);
		hibernateProperties.setProperty("hibernate.show_sql", hibernateShowSql);
		hibernateProperties.setProperty("hibernate.default_schema", hibernateBufferSchema);
		hibernateProperties.setProperty("hibernate.connection.autocommit","false");
		hibernateProperties.setProperty("javax.persistence.validation.mode", "none");
    	sb.setHibernateProperties(hibernateProperties);
    	return sb;
    }
    
    @Bean(name="publicSessionFactory")
    public LocalSessionFactoryBean publicSessionFactory() {
    	LocalSessionFactoryBean sb = new LocalSessionFactoryBean(); 
    	sb.setDataSource(dataSource()); 
    	sb.setAnnotatedClasses(new Class[]{
    			OccurrenceRawModel.class,OccurrenceModel.class,
    			ImportLogModel.class, ResourceModel.class});

		Properties hibernateProperties = new Properties();
		hibernateProperties.setProperty("hibernate.dialect", hibernateDialect);
		hibernateProperties.setProperty("hibernate.show_sql", hibernateShowSql);
		hibernateProperties.setProperty("javax.persistence.validation.mode", "none");
    	sb.setHibernateProperties(hibernateProperties);
    	return sb;
    }
    
    @Bean(name="bufferTransactionManager")
    public HibernateTransactionManager hibernateTransactionManager(){
    	HibernateTransactionManager htmgr = new HibernateTransactionManager();
		htmgr.setSessionFactory(bufferSessionFactory().getObject());
    	return htmgr;
    }
    
    @Bean(name="publicTransactionManager")
    public HibernateTransactionManager publicHibernateTransactionManager(){
    	HibernateTransactionManager htmgr = new HibernateTransactionManager();
		htmgr.setSessionFactory(publicSessionFactory().getObject());
    	return htmgr;
    }
    
    //---VIEW MODEL---
	@Bean
	public HarvesterViewModel harvesterViewModel(){
		return null;
	}
	
    //---JOB---
	//Nodes should not initiate jobs
	@Bean
	public ImportDwcaJob importDwcaJob(){
		return null;
	}
	@Bean
	public MoveToPublicSchemaJob moveToPublicSchemaJob(){
		return null;
	}
	@Bean
	public ComputeUniqueValueJob computeUniqueValueJob(){
		return null;
	}
	
	//---STEP---
	@Bean(name="streamEmlContentStep")
	public ProcessingStepIF streamEmlContentStep(){
		return null;
	}
	@Bean(name="streamDwcContentStep")
	public ProcessingStepIF streamDwcContentStep(){
		return null;
	}
	@Bean(name="insertRawOccurrenceStep")
	public ProcessingStepIF insertRawOccurrenceStep(){
		return new InsertRawOccurrenceStep();
	}
	
	@Bean(name="processInsertOccurrenceStep")
	public ProcessingStepIF processInsertOccurrenceStep(){
		return new ProcessInsertOccurrenceStep();
	}
	
	@Bean(name="insertResourceContactStep")
	public ProcessingStepIF insertResourceContactStep(){
		return new InsertResourceContactStep();
	}
	
	@Bean(name="updateResourceContactStep")
	public ProcessingStepIF updateResourceContactStep(){
		return null;
	}
	
	@Bean(name="streamOccurrenceForStatsStep")
	public ProcessingStepIF streamOccurrenceForStatsStep(){
		return null;
	}
	
	//---Unused TASK in processing node---
	@Bean
	public ItemTaskIF prepareDwcaTask(){
		return null;
	}
	@Bean
	public ItemTaskIF cleanBufferTableTask(){
		return null;
	}
	@Bean
	public ItemTaskIF computeGISDataTask(){
		return null;
	}
	@Bean
	public ItemTaskIF checkProcessingCompletenessTask(){
		return null;
	}
	@Bean
	public ItemTaskIF getResourceInfoTask(){
		return null;
	}
	@Bean
	public ItemTaskIF findUsedDwcaTermTask(){
		return null;
	}
	
	//---PROCESSOR wiring---
	@Bean(name="lineProcessor")
	public ItemProcessorIF<OccurrenceRawModel, OccurrenceRawModel> lineProcessor(){
		return new DwcaLineProcessor();
	}
	
	@Bean(name="occurrenceProcessor")
	public ItemProcessorIF<OccurrenceRawModel, OccurrenceModel> occurrenceProcessor(){
		return new OccurrenceProcessor();
	}
	
	@Bean(name="resourceContactProcessor")
	public ItemProcessorIF<Eml, ResourceContactModel> resourceContactProcessor(){
		return new ResourceContactProcessor();
	}
	
	//---READER wiring---
	@Bean
	public ItemReaderIF<OccurrenceRawModel> dwcaItemReader(){
		return new DwcaItemReader();
	}
	@Bean
	public ItemReaderIF<Eml> dwcaEmlReader(){
		return null;
	}
	
	//---WRITER wiring---
	@Bean(name="rawOccurrenceWriter")
	public ItemWriterIF<OccurrenceRawModel> rawOccurrenceWriter(){
		return new RawOccurrenceHibernateWriter();
	}
	
	@Bean(name="occurrenceWriter")
	public ItemWriterIF<OccurrenceModel> occurrenceWriter(){
		return new OccurrenceHibernateWriter();
	}
	
	@Bean(name="resourceContactWriter")
	public ItemWriterIF<ResourceContactModel> resourceContactHibernateWriter(){
		return new ResourceContactHibernateWriter();
	}
	
	//---Config---
	@Bean
	public HarvesterConfigIF harvesterConfig(){
		//currently not used by the nodes
		return null;
	}
	
	/**
	 * node should not use this
	 * @return
	 */
	@Bean
	public JMSWriter jmsWriter(){
		return null;
	}
	
	@Bean(name="jmsConsumer")
	public JMSConsumer jmsConsumer(){
		return new JMSConsumer(jmsBrokerUrl);
	}
	
}