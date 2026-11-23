package edu.campus.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.campus.common.ApiException;
import java.util.HashSet;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class FormService {
    private final ObjectMapper json;
    private static final Set<String> TYPES = Set.of("text","textarea","select","number","date","radio","checkbox");
    public FormService(ObjectMapper json) { this.json = json; }
    public JsonNode parse(String value) {
        try { return json.readTree(value == null ? "{}" : value); }
        catch (Exception e) { throw ApiException.bad("表单JSON格式不正确"); }
    }
    public String stringify(JsonNode node) {
        if (node == null) return "{}";
        return node.toString();
    }
    public JsonNode validateSchema(JsonNode schema) {
        if (schema == null || schema.isNull()) {
            ObjectNode empty = json.createObjectNode(); empty.put("version",1); empty.putArray("fields"); return empty;
        }
        if (!schema.isObject() || !schema.path("fields").isArray() || schema.path("fields").size()>20)
            throw ApiException.bad("表单需要fields数组，最多20个字段");
        Set<String> keys = new HashSet<>();
        for (JsonNode f : schema.path("fields")) {
            String key=f.path("key").asText(), type=f.path("type").asText();
            if (!key.matches("[A-Za-z][A-Za-z0-9_]{0,31}") || !keys.add(key) || !TYPES.contains(type)
                    || f.path("label").asText().isBlank() || f.path("label").asText().length()>50)
                throw ApiException.bad("表单字段标识、名称或类型不正确");
            if (f.path("maxLength").asInt(500)<1 || f.path("maxLength").asInt(500)>2000)
                throw ApiException.bad("字段长度限制应在1至2000之间");
            if (Set.of("select","radio","checkbox").contains(type)) {
                if (!f.path("options").isArray() || f.path("options").isEmpty() || f.path("options").size()>30)
                    throw ApiException.bad("选择类字段需要1至30个选项");
                Set<String> options=new HashSet<>();
                for(JsonNode o:f.path("options")) if(!o.isTextual()||o.asText().isBlank()||o.asText().length()>100||!options.add(o.asText())) throw ApiException.bad("表单选项不能为空或重复");
            }
        }
        return schema;
    }
    public void validateData(JsonNode schema, JsonNode data, boolean draft) {
        if (data==null || !data.isObject() || data.toString().length()>30000) throw ApiException.bad("报名表单格式错误或内容过长");
        Set<String> keys=new HashSet<>();
        for (JsonNode field : schema.path("fields")) {
            String key=field.path("key").asText(), label=field.path("label").asText(),type=field.path("type").asText();
            keys.add(key); JsonNode value=data.get(key);
            boolean empty=value==null||value.isNull()||(value.isTextual()&&value.asText().isBlank())||(value.isArray()&&value.isEmpty());
            if (empty) {
                if (!draft && field.path("required").asBoolean()) throw ApiException.bad("请填写"+label);
                continue;
            }
            if ("number".equals(type)) { if(!value.isNumber()) throw ApiException.bad(label+"必须为数字"); }
            else if ("checkbox".equals(type)) {
                if(!value.isArray()) throw ApiException.bad(label+"格式不正确");
                for(JsonNode item:value) if(!contains(field.path("options"),item))throw ApiException.bad(label+"选项无效");
            } else {
                if(!value.isTextual()||value.asText().length()>field.path("maxLength").asInt(500))throw ApiException.bad(label+"格式错误或长度超限");
                if(Set.of("select","radio").contains(type)&&!contains(field.path("options"),value))throw ApiException.bad(label+"选项无效");
                if("date".equals(type))try{java.time.LocalDate.parse(value.asText());}catch(Exception e){throw ApiException.bad(label+"日期无效");}
            }
        }
        data.fieldNames().forEachRemaining(key->{if(!keys.contains(key))throw ApiException.bad("报名包含未定义的字段");});
    }
    private boolean contains(JsonNode options,JsonNode value){for(JsonNode item:options)if(item.equals(value))return true;return false;}
}
