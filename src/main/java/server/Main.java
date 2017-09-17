/*******************************************************************************
 * Copyright (c) 2017 - IT Center for Clinical Research, University of Luebeck
 * Noemi Deppenwiese, Hannes Ulrich
 ******************************************************************************/
package server;

import static spark.Spark.get;
import static spark.Spark.post;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.hl7.fhir.dstu3.model.Questionnaire;
import org.hl7.fhir.dstu3.model.codesystems.IssueSeverity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import JsonModels.Errors;
import ca.uhn.fhir.context.FhirContext;
import converter.ConversionOutcome;
import converter.Issue;
import converter.QuestionnaireConverter;
import spark.ModelAndView;
import spark.Spark;
import spark.servlet.SparkApplication;
import spark.template.freemarker.FreeMarkerEngine;

public class Main implements SparkApplication {
	
	final static String baseUrl = "fhir2emx";

	final static Logger logger = LoggerFactory.getLogger(Main.class);

	private static FhirContext fhirContext;

	private static Gson gson;

	public static void main(String[] args) {

		// Prepare and start EMX file Remover
		Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(new OutdatedFilesRemover(5), 1, 5,
				TimeUnit.MINUTES);

		new Main().init();
	}

	@Override
	public void init() {

		// Create FhirContext once
		fhirContext = FhirContext.forDstu3();

		gson = new Gson();

		Spark.staticFileLocation("public/");

		// Initial GUI
		get(baseUrl+"/", (req, res) -> {

			Map<String, Object> attributes = new HashMap<>();
			attributes.put("fhir_url", "https://fhirtest.uhn.ca/baseDstu3");
			attributes.put("ques_id", "12345");
			return new FreeMarkerEngine().render(new ModelAndView(attributes, "index.ftl.html"));
		});

		// Fetch and display questionnaire
		get(baseUrl+"/gui_ques", (req, res) -> {

			String fhir_url = req.queryParams("fhir-url");
			String ques_id = req.queryParams("ques-id");

			FHIRResourceFetcher fetch = new FHIRResourceFetcher(fhirContext, fhir_url);
			Questionnaire ques = fetch.fetchQuestionnaire(ques_id);

			Map<String, Object> attributes = new HashMap<>();
			attributes.put("fhir_url", fhir_url);
			attributes.put("ques_id", ques_id);
			if (ques == null) {
				attributes.put("questionnaire_raw", "ERROR: Could not fetch Questionnaire. Is URL/ID right?");
				attributes.put("no_ques","true");
			} else {
				attributes.put("questionnaire_raw",
						fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(ques));
			}

			attributes.put("molg_url", "http://molgenis.example.com");
			attributes.put("molg_user", "user");
			attributes.put("mplg_pw", "password");

			return new FreeMarkerEngine().render(new ModelAndView(attributes, "index.ftl.html"));
		});

		// Transform GUI input with MOLGENIS namechecking
		post(baseUrl+"/gui-transform", (req, res) -> {
			Map<String, Object> attributes = new HashMap<>();
			
			Errors errors = new Errors();

			// Fetch Questionnaire again
			String fhir_url = req.queryParams("fhir-url");
			String ques_id = req.queryParams("ques-id");

			//Get with tags
			boolean withTags = (req.queryParams("with-tags") != null);
			
			attributes.put("fhir_url", fhir_url);
			attributes.put("ques_id", ques_id);

			FHIRResourceFetcher fetch = new FHIRResourceFetcher(fhirContext, fhir_url);
			Questionnaire ques = fetch.fetchQuestionnaire(ques_id);

			// Log into MOLGENIS
			String molgenisUrl = req.queryParams("molg-url");
			String molgenisUser = req.queryParams("molg-user");
			String molgenisPW = req.queryParams("molg-pw");
			
			attributes.put("molg_url", molgenisUrl);
			attributes.put("molg_user", molgenisUser);
			attributes.put("molg_pw", molgenisPW);
			
			MOLGENISServerConnector connector = new MOLGENISServerConnector(molgenisUrl);
			if (!connector.serverAlive()) {
				res.status(502);
				errors.issues.add(new Issue(IssueSeverity.FATAL, "molgenisurl", "MOLGENIS not responding"));
				attributes.put("errors", errors.issues);
				return new FreeMarkerEngine().render(new ModelAndView(attributes, "index.ftl.html"));
			}
			if (!connector.login(molgenisUser, molgenisPW)) {
				res.status(401);
				errors.issues.add(new Issue(IssueSeverity.FATAL, "molgenisuser,molgenispw", "MOLGENIS Login failed"));
				attributes.put("errors", errors.issues);
				return new FreeMarkerEngine().render(new ModelAndView(attributes, "index.ftl.html"));
			}

			QuestionnaireConverter converter = new QuestionnaireConverter(ques, fetch, connector);
			converter.convert();
			ConversionOutcome outcome = converter.getOutcome(withTags);

			Map<String, String> emxTables = outcome.getEmxTables();
			
			//Write file
			String id = FileHandler.saveAsFile(emxTables);

			attributes.put("upload", true);
			
			if (!outcome.getIssues().isEmpty()) {

				errors.issues.addAll(outcome.getIssues());

			}
			// Render fancy HTML tables
			List<TableResult> tables = new ArrayList<TableResult>();
			// For each table
			for (String name : emxTables.keySet()) {
				String[] rows = emxTables.get(name).split("\\r?\\n");
				ArrayList<String[]> table = new ArrayList<String[]>();
				// For each line
				for (String line : rows) {
					// add all entries
					table.add(line.split("\\t"));
				}

				tables.add(new TableResult(name, table));
			}

			attributes.put("emx_id", id);
			attributes.put("errors", errors.issues);
			attributes.put("result_tables", tables);

			return new FreeMarkerEngine().render(new ModelAndView(attributes, "index.ftl.html"));
		});

		// Transform GUI input without MOLGENIS namechecking
		post(baseUrl+"/gui-transform-no-molg", (req, res) -> {

			Map<String, Object> attributes = new HashMap<>();

			// Fetch Questionnaire again
			String fhir_url = req.queryParams("fhir-url");
			String ques_id = req.queryParams("ques-id");

			attributes.put("fhir_url", fhir_url);
			attributes.put("ques_id", ques_id);

			FHIRResourceFetcher fetch = new FHIRResourceFetcher(fhirContext, fhir_url);
			Questionnaire ques = fetch.fetchQuestionnaire(ques_id);
			
			//Get with tags
			boolean withTags = (req.queryParams("with-tags") != null);
			
			QuestionnaireConverter converter = new QuestionnaireConverter(ques, fetch, null);
			converter.convert();
			ConversionOutcome outcome = converter.getOutcome(withTags);

			if (!outcome.getIssues().isEmpty()) {

				attributes.put("errors", outcome.getIssues());

			}
			Map<String, String> emxTables = outcome.getEmxTables();
			
			//Write file
			String id = FileHandler.saveAsFile(emxTables);
			attributes.put("emx_id", id);

			// Render fancy HTML tables
			List<TableResult> tables = new ArrayList<TableResult>();
			// For each table
			for (String name : emxTables.keySet()) {
				String[] rows = emxTables.get(name).split("\\r?\\n");
				ArrayList<String[]> table = new ArrayList<String[]>();
				// For each line
				for (String line : rows) {
					// add all entries
					table.add(line.split("\\t"));
				}

				tables.add(new TableResult(name, table));
			}

			attributes.put("result_tables", tables);

			return new FreeMarkerEngine().render(new ModelAndView(attributes, "index.ftl.html"));
		});
		
		// Post File with given id to MOLGENIS
		post(baseUrl+"/molgenis-import", (req, res) -> {
			Errors errors = new Errors();
			Map<String, Object> attributes = new HashMap<>();
			String fhir_url = req.queryParams("fhir-url");
			String ques_id = req.queryParams("ques-id");

			attributes.put("fhir_url", fhir_url);
			attributes.put("ques_id", ques_id);
			
			String id = req.queryParams("id");
			// Log into MOLGENIS
			String molgenisUrl = req.queryParams("molg-url");
			String molgenisUser = req.queryParams("molg-user");
			String molgenisPW = req.queryParams("molg-pw");

			MOLGENISServerConnector connector = new MOLGENISServerConnector(molgenisUrl);
			if (!connector.serverAlive()) {
				res.status(502);
				errors.issues.add(new Issue(IssueSeverity.FATAL, "molgenisurl", "MOLGENIS not responding"));
				attributes.put("errors", errors.issues);
				return new FreeMarkerEngine().render(new ModelAndView(attributes, "index.ftl.html"));
			}
			if (!connector.login(molgenisUser, molgenisPW)) {
				res.status(401);
				errors.issues.add(new Issue(IssueSeverity.FATAL, "molgenisuser,molgenispw", "MOLGENIS Login failed"));
				attributes.put("errors", errors.issues);
				return new FreeMarkerEngine().render(new ModelAndView(attributes, "index.ftl.html"));
			}
			logger.debug("Invoking connector.postExistingFile with id "+id);
			int status = connector.postExistingFile(id);
			if(status == 404){
				errors.issues.add(new Issue(IssueSeverity.FATAL, "EMX File" , "Unable to find file with id "+id));
				res.status(404);
			}
			if(status == 201){
				res.status(200);
				errors.issues.add(new Issue(IssueSeverity.INFORMATION, "MOLGENIS Response" , "Upload was sucessfull (MOLGENIS responded with 201)"));
			}else{
				res.status(status);
				errors.issues.add(new Issue(IssueSeverity.ERROR, "MOLGENIS Response" , "Upload failed (MOLGENIS responded with "+status+")"));
			}
			attributes.put("errors", errors.issues);
			return new FreeMarkerEngine().render(new ModelAndView(attributes, "index.ftl.html"));
		});
		
		//Supply EMX Files
		get(baseUrl+"/emx", (req, res) -> {
			String id = req.queryParams("id");
			res.redirect("tables/"+id+".zip");
			return null;
		});

		// Server API
		get(baseUrl+"/api", (req, res) -> {
			res.redirect("apiDoku.html");
			return null;
		});

		post(baseUrl+"/api/convert", (req, res) -> {
			logger.debug("Processing API convert request");
			Errors errors = new Errors();
			
			if (req.contentType().contains("application/json")) {
				JsonModels.IncomingRequest request = null;
				try {
					// Parse JSON
					request = gson.fromJson(req.body(), JsonModels.IncomingRequest.class);
				} catch (Exception e) {
					res.status(400);
					errors.issues.add(new Issue(IssueSeverity.FATAL, "body", "Does not contain required fields"));
					res.type("application/json");
					return errors;
				}
				// Test if questionnaire field do exist
				if (request.serverbase == null || request.questionnaireid == null) {
					res.status(400);
					errors.issues.add(new Issue(IssueSeverity.FATAL, "body", "Does not contain required fields"));
					res.type("application/json");
					return errors;
				}

				Questionnaire ques = null;
				FHIRResourceFetcher fetch = null;
				try {
					// Try to fetch Questionnaire
					fetch = new FHIRResourceFetcher(fhirContext, request.serverbase);
					ques = fetch.fetchQuestionnaire(request.questionnaireid);
				} catch (Exception e) {
					res.status(404);
					errors.issues.add(
							new Issue(IssueSeverity.FATAL, "serverbase,questionnaireid", "Questionnaire not found"));
					res.type("application/json");
					return errors;
				}
				// Questionnaire not found
				if (ques == null) {
					res.status(404);
					errors.issues.add(
							new Issue(IssueSeverity.FATAL, "serverbase,questionnaireid", "Questionnaire not found"));
					res.type("application/json");
					return errors;
				}

				// MOLGENIS
				MOLGENISServerConnector connector = null;
				// If MOLGENIS params exist, login
				if (!(request.molgenisurl == null || request.molgenisuser == null || request.molgenispw == null)) {
					logger.debug("Logging into MOLGENIS at "+request.molgenisurl);
					// Login to MOLGENIS
					connector = new MOLGENISServerConnector(request.molgenisurl);
					if (!connector.serverAlive()) {
						res.status(502);
						errors.issues.add(new Issue(IssueSeverity.FATAL, "molgenisurl", "MOLGENIS not responding"));
						res.type("application/json");
						return errors;
					}
					if (!connector.login(request.molgenisuser, request.molgenispw)) {
						res.status(401);
						errors.issues.add(new Issue(IssueSeverity.FATAL, "molgenisuser,molgenispw", "MOLGENIS Login failed"));
						res.type("application/json");
						return errors;
					}
				}
				logger.trace("Preparing conversion...");
				QuestionnaireConverter converter = new QuestionnaireConverter(ques, fetch, connector);
				converter.convert();
				ConversionOutcome outcome = converter.getOutcome(request.generateTags);
				logger.trace("Conversion finished.");
				

				if (outcome.hasErrors()) {
					// prepare return
					res.status(422);
					errors.issues.add(new Issue(IssueSeverity.FATAL, "questionnaire",
							"Not able to generate valid EMX due to parsing errors"));
					errors.issues.addAll(outcome.getIssues());
					res.type("application/json");
					return errors;
				} else {
					// Sucess
					res.status(200);
					if (!outcome.getIssues().isEmpty()) {
						errors.issues.add(new Issue(IssueSeverity.INFORMATION, null,
								"Valid EMX was generated but there were some Issues"));
						errors.issues.addAll(outcome.getIssues());
						res.type("application/json");
					} else {
						// Everything OK
						errors.issues.add(new Issue(IssueSeverity.INFORMATION, null,
								"Valid EMX was generated without Issues"));
						res.type("application/json");
					}
				}
				
				//If we're here no serious errors were reported, try posting to MOLGENIS
				if(connector != null){
					//Post tables to MOLGENIS
					int status = connector.postEMXByFile(outcome.getEmxTables());
					connector.logout();
					if(status == 201){
						res.status(200);
						errors.issues.add(new Issue(IssueSeverity.INFORMATION, "MOLGENIS",
								"Sucessfully posted EMX to MOLGENIS (Response had status 201)"));
					}else{
						res.status(status);
						errors.issues.add(new Issue(IssueSeverity.FATAL, "MOLGENIS",
								"There was an error posting the EMX to MOLGENIS (Response had status "+status+")"));
					}
				}else{
					res.status(400);
					errors.issues.add(new Issue(IssueSeverity.INFORMATION, "Request",
									"Nothing was posted to MOLGENIS because some Paramenters were missing"));
				}
				res.type("application/json");
				return errors;
				
			} else {
				// Error
				res.status(400);
				errors.issues.add(new Issue(IssueSeverity.FATAL, "content-header", "Must be application/json"));
				res.type("application/json");
				return errors;
			}
			
			
		}, new JsonTransformer());
	}
}
