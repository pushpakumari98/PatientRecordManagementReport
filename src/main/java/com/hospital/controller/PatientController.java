package com.hospital.controller;
import com.hospital.dto.PatientRequest;
import com.hospital.entity.*;
import com.hospital.service.DoctorService;
import com.hospital.service.PatientHistoryService;
import com.hospital.service.PatientService;
import com.hospital.service.ReportService;
import com.hospital.serviceimpl.ExportExcel;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import net.sf.jasperreports.engine.JRException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import static org.apache.catalina.util.XMLWriter.NO_CONTENT;
import static org.springframework.http.HttpStatus.CREATED;

@RestController  //To create REST ful web services. It combines @Controller and @ResponseBody,i.e every method in a class annotated with @RestController will return a domain object directly in the HTTP response body, typically as JSON or XML.
@RequestMapping("/api")
public class PatientController {
        @Autowired
        private PatientService patientService;

        @Autowired
        private ExportExcel exportExcel;

        @Autowired
        private PatientHistoryService patientHistoryService;
        //localhost:8081/api/patient

        @Autowired
        private DoctorService doctorService;

    @Autowired
    private ReportService reportService;

    @GetMapping("/report/{format}")
    public String generatePatientDetailsReport(@PathVariable String format) throws JRException, FileNotFoundException {
        return reportService.exportPatientDetails(format);
    }


        //POST //GET//DELETE//PUT
        @PostMapping("/patient") // A new patient will get created
        public ResponseEntity savePatient (@Valid @RequestBody PatientRequest patient, BindingResult result)
        {
            if (result.hasErrors()) {
                List<String> errorList = result.getAllErrors().stream()
                        .map(DefaultMessageSourceResolvable::getDefaultMessage)
                        .collect(Collectors.toList());
                return ResponseEntity.status(CREATED).body(errorList);
            }
            //TODO: check the duplicate value
            Patient p = new Patient();
            p.setName(patient.getName());
            p.setAge(patient.getAge());
            p.setPhone(patient.getPhone());
            p.setDob(patient.getDob());
            p.setAadhar(patient.getAadhar());
            p.setAddmissionDate(new Date());
            Patient createdPatient = null;

            try {
                Patient patient1 = patientService.findByAadhar(patient.getAadhar());

                if (patient1 == null) {
                    createdPatient = patientService.savePatient(p);   //
                } else {
                    return ResponseEntity.status(CREATED).body("Patient already exist!! Your Patient Id is: " + patient1.getId());
                }
            }catch  (NoSuchElementException e){
                createdPatient = patientService.savePatient(p);
            }
            catch (Exception e) {
                return ResponseEntity.status(CREATED).body("Something went wrong. Please check with Admin!!");
            }
            return ResponseEntity.status(CREATED).body(createdPatient);
        }

        @GetMapping("/getPatient")  //All the admitted patient will get retrieved
        public ResponseEntity getAllPatient() {
            List<Patient> patientList = null;
            try {
                patientList = patientService.getAllPatient();
            } catch (Exception e) {
                e.printStackTrace();
                return ResponseEntity.ok().body("Something Went Wrong!");
            }

            return ResponseEntity.ok().body(patientList);
        }

        @GetMapping("/getPatient/{patientId}")  //This api find the patient using patient Id.
        public ResponseEntity getPatientById (@PathVariable Long patientId){

            System.out.println("patientId " + patientId);
            Patient patient = null;
            try {
                patient = patientService.getPatientByPatientId(patientId);
            }catch (NoSuchElementException e){
                return ResponseEntity.ok().body("Patient Does Not Exist!");
            } catch (Exception e) {
                e.printStackTrace();
                return ResponseEntity.ok().body("Something Went Wrong!");
            }
            return ResponseEntity.ok().body(patient);
        }

        @DeleteMapping("/deletePatient/{patientId}") //This api deletes a patient using patient id
        @Transactional
        public ResponseEntity deletePatientById (@RequestParam Long patientId){
            Patient patient = null;
            System.out.println("patientId " + patientId);
            try {
                Patient patient2 = null;
                Address add = null;
                try {
                   patient2 = patientService.getPatientByPatientId(patientId);
                   if(patient2.getAddress()!=null){
                       add = patient2.getAddress();
                   }
                } catch (NoSuchElementException e) {
                    return ResponseEntity.status(NO_CONTENT).body("Patient Does Not Exist!!!");
                }
                if(patient2 == null){
                    return ResponseEntity.status(NO_CONTENT).body("Patient Does Not Exist!!!");
                }
                PatientHistory patientHistory = new PatientHistory();
                AddressHistory addHis = new AddressHistory();
                patientHistory.setId(patient2.getId());
                patientHistory.setName(patient2.getName());
                patientHistory.setDob(patient2.getDob());
                patientHistory.setAadhar(patient2.getAadhar());
                patientHistory.setAge(patient2.getAge());
                patientHistory.setPhone(patient2.getPhone());
                patientHistory.setAddmissionDate(patient2.getAddmissionDate());
                patientHistory.setDischargeDate(new Date());
               if(add!=null){
                   addHis.setId(add.getId());
                   addHis.setCity(add.getCity());
                   addHis.setAddType(add.getAddType());
                   addHis.setCountry(add.getCountry());
                   addHis.setState(add.getState());
                   addHis.setPin(add.getPin());
               }
                patientHistory.setAddress(addHis);

                patientService.deletePatientById(patientId);


                patientHistoryService.saveDeletedPatient(patientHistory);  //wro

            } catch (Exception e) {
                e.printStackTrace();
                return ResponseEntity.ok().body("Something Went Wrong!");
            }

            return new ResponseEntity(HttpStatus.NO_CONTENT);
        }

        @PutMapping("/updatePatient") //to update any data  we use @Putmapping

        @RequestMapping(value = "/updatePatient/{aadhar}", method = RequestMethod.PUT)
        public ResponseEntity updatePatient (@RequestBody PatientRequest patient, String aadhar, BindingResult result){


            try {
                Patient dbPatient = patientService.findByAadhar(aadhar);
                if (dbPatient == null) {
                    return ResponseEntity.badRequest().body("Patient does not exist!!Please Proceed for registration.");
                } else {
                    //patient
                    //dbPatient null
                    // null != null
                    // TRUE &&  !false
                    if (patient.getName() != null && !patient.getName().equalsIgnoreCase(dbPatient.getName())) {
                        dbPatient.setName(patient.getName());
                    }
                    if (patient.getAadhar() != null && !patient.getAadhar().equalsIgnoreCase(dbPatient.getAadhar())) {
                        dbPatient.setAadhar(patient.getAadhar());
                    }
                    if (patient.getDob() != null && patient.getDob().after(dbPatient.getDob()) || patient.getDob().before(patient.getDob())) {
                        dbPatient.setDob(patient.getDob());
                    }
                    if (patient.getAge() != null) {
                        dbPatient.setAge(patient.getAge());
                    }
                    try {
                        Patient updatedPatient = patientService.savePatient(dbPatient);
                    } catch (Exception e) {
                        return ResponseEntity.badRequest().body("Something went wrong in update.");
                    }
                }
            } catch (Exception e) {
                return ResponseEntity.badRequest().body("Something went wrong in update.");
            }
            return ResponseEntity.ok().body("Patient Updated Successfully.");
        }

        @GetMapping("/patient/{patientId}/doctor/{doctorId}") //To link a specific patient with a specific doctor in a hospital
        public ResponseEntity linkPatientWithDoctor(@PathVariable Long patientId, @PathVariable Long doctorId){

            Patient patient = null;
            try {
                patient = patientService.getPatientByPatientId(patientId);
            }catch (NoSuchElementException e){
                return ResponseEntity.ok().body("Patient Does Not Exist!");
            } catch (Exception e) {
                e.printStackTrace();
                return ResponseEntity.ok().body("Something Went Wrong!");
            }

            Doctor doctor = null;
            try {
                doctor = doctorService.getDoctorById(doctorId);
            }catch (NoSuchElementException e){
                return ResponseEntity.ok().body("Doctor Does Not Exist!");
            } catch (Exception e) {
                return ResponseEntity.ok().body("Something Went Wrong!");
            }
            if(doctor!=null){
                doctor.setPatient(patient);
            }
            doctorService.saveDoctor(doctor);
            return ResponseEntity.ok().body("Doctor has been assigned Successfully.");
        }


    @GetMapping("/export-patient")
    public ResponseEntity exportPatient(HttpServletResponse response) throws IOException, IOException {
        response.setContentType("application/octet-stream");
        String headerKey = "Content-Disposition";
        String headerValue = "attachment; filename=Patient_Info.xlsx";
        response.setHeader(headerKey, headerValue);
        exportExcel.exportPatientDataToExcel(response);
        return new ResponseEntity(HttpStatus.OK);
    }

}
