package xyz.mobi.employeehelpdesk.service;

public interface SlaEvaluationService {

    void processSlaBreaches();

    void processSlaWarnings();

    void evaluateBreach(Long slaInstanceId);

    void evaluateWarning(Long slaInstanceId);
}
