<template>
    <FormSelect v-model="value" :values="sourceLocalizations" #default="{ value: v}">
        <span v-if="!v">Toutes</span>
        <span v-else><Flag :lang="v.code" /> {{ v.name }}</span>
    </FormSelect>
</template>

<script lang="ts" setup>
import {
    Flag,
    Localization,
    LocalizationCode,
    localizationCodes,
    localizations as allLocalizations
} from '@/localization';
import {computed} from 'vue';
import FormSelect from "@components/form/FormSelect.vue";

type LocalizationOrAll = Localization | 'all';
type LocalizationCodeOrAll = LocalizationCode | 'all';

interface Props {
    modelValue?: LocalizationCodeOrAll;
    localizations?: LocalizationCode[];
}
interface Emits {
    (e: 'update:modelValue', value: LocalizationCodeOrAll): void;
}

const props = withDefaults(defineProps<Props>(), {
    localizations: () => localizationCodes
});
const emit = defineEmits<Emits>();

const value = computed({
    get: () => props.modelValue === 'all' || props.modelValue === undefined ? undefined : allLocalizations[props.modelValue],
    set: (v?: LocalizationOrAll) => emit('update:modelValue', v === 'all' || v === undefined ? 'all' : v.code)
})

const sourceLocalizations = computed(() => Object.values(allLocalizations).filter(l => props.localizations.includes(l.code)));
</script>
