/*
* The contents of this file are subject to the Mozilla Public License
* Version 1.1 (the "License"); you may not use this file except in
* compliance with the License. You may obtain a copy of the License at
* http://www.mozilla.org/MPL/
* 
* Software distributed under the License is distributed on an "AS IS"
* basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
* License for the specific language governing rights and limitations under
* the License.
* 
* The Original Code is OpenELIS code.
* 
* Copyright (C) The Minnesota Department of Health.  All Rights Reserved.
*
* Contributor(s): CIRG, University of Washington, Seattle WA.
*/
package us.mn.state.health.lims.reports.action.implementation.reportBeans;


import static us.mn.state.health.lims.reports.action.implementation.reportBeans.CSVColumnBuilder.Strategy.NONE;
import us.mn.state.health.lims.reports.action.implementation.Report.DateRange;
import static us.mn.state.health.lims.reports.action.implementation.reportBeans.CSVColumnBuilder.Strategy.TEST_RESULT;

/**
 * @author pahill (pahill@uw.edu)
 * @since May 16, 2011
 */
public class EIDColumnBuilder extends CIColumnBuilder {

    /**
     * @param dateRange
     * @param projectStr
     */
    public EIDColumnBuilder(DateRange dateRange, String projectStr) {
        super(dateRange, projectStr);
    }
    
    /**
     * This is the order we want them in the CSV file.
     */
    protected void defineAllReportColumns() {
        defineBasicColumns();
        
        add("DNA PCR"     ,"DNA PCR", TEST_RESULT);
        add("started_date"     ,"STARTED_DATE", NONE);
        add("completed_date"     ,"COMPLETED_DATE", NONE);
        add("released_date"     ,"RELEASED_DATE", NONE);
        add("patient_oe_id"     ,"PATIENT_OE_ID", NONE);// a means to check unknown patient with id=1
        
        add("nameOfSampler"     ,"NOMPREV", NONE);
        add("nameOfRequestor"   ,"NOMMED",  NONE);
        add("whichPCR"                ,"whichPCR"                       );     
        add("reasonForSecondPCRTest"  ,"reasonForSecondPCRTest"         );     
        add("eidInfantPTME"           ,"eidInfantPTME"                  );     
        add("eidTypeOfClinic"         ,"eidTypeOfClinic", new EIDTypeOfClinicStrategy()  );     
        add("eidInfantSymptomatic"    ,"eidInfantSymptomatic"           );     
        add("eidMothersHIVStatus"     ,"eidMothersHIVStatus"            );     
        add("eidMothersARV"           ,"eidMothersARV"                  );     
        add("eidInfantsARV"           ,"eidInfantsARV"                  );     
        add("eidInfantCotrimoxazole"  ,"eidInfantCotrimoxazole"         );     
        add("eidHowChildFed"          ,"eidHowChildFed"                 );     
        add("eidStoppedBreastfeeding" ,"eidStoppedBreastfeeding"        );   
       // addAllResultsColumns();
    }
    
    /**
     * @return the SQL for (nearly) one big row for each sample in the date range for the particular project.
     */
    public void makeSQL1() {
        query = new StringBuilder();
        String lowDatePostgres =  postgresDateFormat.format(dateRange.getLowDate());
        String highDatePostgres = postgresDateFormat.format(dateRange.getHighDate());
        query.append(SELECT_SAMPLE_PATIENT_ORGANIZATION );
        // all crosstab generated tables need to be listed in the SELECT column list and in the WHERE clause at the bottom
        query.append(SELECT_ALL_DEMOGRAPHIC_AND_RESULTS);
        // more cross tabulation of other columns goes where

        // ordinary lab (sample and patient) tables
        query.append(FROM_SAMPLE_PATIENT_ORGANIZATION );

        // all observation history from expressions
        appendObservationHistoryCrosstab(lowDatePostgres, highDatePostgres);
        appendResultCrosstab(lowDatePostgres, highDatePostgres);

        // and finally the join that puts these all together. Each cross table should be listed here otherwise it's not in the result and you'll get a full join
        query.append(buildWhereSamplePatienOrgSQL(lowDatePostgres, highDatePostgres)
                        // insert joining of any other crosstab here.
                        // insert joining of any other crosstab here.
                        + "\n AND s.id = demo.samp_id "
                        + "\n AND s.id = result.samp_id "                        
                        + "\n ORDER BY s.accession_number "
                        );
        // no don't insert another crosstab or table here, go up before the main WHERE clause

        return;
    }
    public void makeSQL(){
    	//makeSQL1(); old one without analysis.released_date
    	makeSQL2();// new one with analysis.released_date
    }
	public void makeSQL2() {
	
	    query = new StringBuilder();
	    String lowDatePostgres =  postgresDateFormat.format(dateRange.getLowDate());
	    String highDatePostgres = postgresDateFormat.format(dateRange.getHighDate());
	    query.append( SELECT_SAMPLE_PATIENT_ORGANIZATION );
	    // all crosstab generated tables need to be listed in the following list and in the WHERE clause at the bottom
	    query.append("\n, pat.id AS patient_oe_id, a.started_date,a.completed_date,a.released_date,a.printed_date, r.value as \"DNA PCR\", demo.* ");
	
	    // ordinary lab (sample and patient) tables
	    query.append( FROM_SAMPLE_PATIENT_ORGANIZATION +
	    		", clinlims.sample_item as si, clinlims.analysis as a, clinlims.result as r ");
	
	    // all observation history values
	    appendObservationHistoryCrosstab(lowDatePostgres, highDatePostgres);
	    // current ARV treatments
	 //   appendRepeatingObservation("currentARVTreatmentINNs", 4,  lowDatePostgres, highDatePostgres);
	    //result
	  //  appendResultCrosstab(lowDatePostgres, highDatePostgres );
	
	    // and finally the join that puts these all together. Each cross table should be listed here otherwise it's not in the result and you'll get a full join
	    query.append( " WHERE "             
	    + "\n a.test_id = 175" 
	    + "\n AND a.status_id = 18" 
	    + "\n AND a.id=r.analysis_id"
	    + "\n AND a.sampitem_id = si.id" 
	    + "\n AND s.id = si.samp_id"
	    + "\n AND s.id=sh.samp_id" 
	    + "\n AND sh.patient_id=pat.id" 
	    + "\n AND pat.person_id = per.id"
	    + "\n AND s.id=so.samp_id" 
	    + "\n AND so.org_id=o.id" 
	    + "\n AND s.id = sp.samp_id" 
	    + "\n AND s.id=demo.s_id"
	  //  + "\n AND s.id = currentARVTreatmentINNs.samp_id"
	    + "\n AND s.collection_date >= date('" + lowDatePostgres + "')" 
	    + "\n AND s.collection_date <= date('" + highDatePostgres + "')"
	    
	    + "\n ORDER BY s.accession_number;");
	    /////////
	    // no don't insert another crosstab or table here, go up before the main WHERE clause

	    return;
	}    
}
