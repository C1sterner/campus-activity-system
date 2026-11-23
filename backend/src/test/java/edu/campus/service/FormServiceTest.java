package edu.campus.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.campus.common.ApiException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FormServiceTest {
    final FormService forms=new FormService(new ObjectMapper());
    final String schema="{\"fields\":[{\"key\":\"name\",\"label\":\"姓名\",\"type\":\"text\",\"required\":true,\"maxLength\":20},{\"key\":\"grade\",\"label\":\"年级\",\"type\":\"select\",\"options\":[\"大一\",\"大二\"]}]}";
    @Test void validForm(){var s=forms.validateSchema(forms.parse(schema));assertDoesNotThrow(()->forms.validateData(s,forms.parse("{\"name\":\"同学\",\"grade\":\"大一\"}"),false));}
    @Test void requiredServerValidation(){assertThrows(ApiException.class,()->forms.validateData(forms.parse(schema),forms.parse("{}"),false));}
    @Test void draftAllowsMissingRequired(){assertDoesNotThrow(()->forms.validateData(forms.parse(schema),forms.parse("{}"),true));}
    @Test void unknownFieldRejected(){assertThrows(ApiException.class,()->forms.validateData(forms.parse(schema),forms.parse("{\"name\":\"甲\",\"role\":\"admin\"}"),false));}
    @Test void invalidOptionRejected(){assertThrows(ApiException.class,()->forms.validateData(forms.parse(schema),forms.parse("{\"name\":\"甲\",\"grade\":\"研究生\"}"),false));}
    @Test void repeatedKeyRejected(){assertThrows(ApiException.class,()->forms.validateSchema(forms.parse("{\"fields\":[{\"key\":\"x\",\"label\":\"甲\",\"type\":\"text\"},{\"key\":\"x\",\"label\":\"乙\",\"type\":\"text\"}]}")));}
}
