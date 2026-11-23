<script setup>
defineProps({fields:{type:Array,default:()=>[]},modelValue:{type:Object,required:true},readonly:Boolean})
</script>
<template><template v-for="field in fields" :key="field.key">
  <el-form-item :label="field.label" :required="field.required">
    <el-select v-if="field.type==='select'" v-model="modelValue[field.key]" :disabled="readonly" placeholder="请选择" style="width:100%"><el-option v-for="option in field.options" :key="option" :label="option" :value="option"/></el-select>
    <el-radio-group v-else-if="field.type==='radio'" v-model="modelValue[field.key]" :disabled="readonly"><el-radio v-for="option in field.options" :key="option" :value="option">{{option}}</el-radio></el-radio-group>
    <el-checkbox-group v-else-if="field.type==='checkbox'" v-model="modelValue[field.key]" :disabled="readonly"><el-checkbox v-for="option in field.options" :key="option" :value="option">{{option}}</el-checkbox></el-checkbox-group>
    <el-input-number v-else-if="field.type==='number'" v-model="modelValue[field.key]" :disabled="readonly"/>
    <el-date-picker v-else-if="field.type==='date'" v-model="modelValue[field.key]" type="date" value-format="YYYY-MM-DD" :disabled="readonly" style="width:100%"/>
    <el-input v-else v-model="modelValue[field.key]" :type="field.type==='textarea'?'textarea':'text'" :rows="3" :maxlength="field.maxLength||500" :disabled="readonly" :placeholder="'请输入'+field.label"/>
  </el-form-item>
</template></template>
