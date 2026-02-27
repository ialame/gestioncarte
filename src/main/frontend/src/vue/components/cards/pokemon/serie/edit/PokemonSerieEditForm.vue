<template>
  <AdvancedForm v-model="value" :small="true">
    <div class="container">
      <AdvancedFormTranslations
          label="Traductions"
          path="translations"
          :localizations="localizationCodes"
          :enforceLanguageGroups="true"
          #default="{path, isLanguageCompatible}">

        <FormRow>
          <AdvancedFormInput
              :path="concatPaths([path, 'name'])"
              label="Nom"
              :disabled="!isLanguageCompatible"
              :class="{ 'opacity-50': !isLanguageCompatible }" />
        </FormRow>

      </AdvancedFormTranslations>
    </div>
    <template #out-of-side>
      <div class="container p-0">
        <div class="d-flex flex-row float-end">
          <slot name="save-buttons" />
          <AdvancedFormSaveButton @save="$emit('save')" />
        </div>
      </div>
    </template>
  </AdvancedForm>
</template>

<script lang="ts" setup>
import {concatPaths} from "@/path";
import FormRow from "@components/form/FormRow.vue";
import {
  AdvancedForm,
  AdvancedFormInput,
  AdvancedFormSaveButton,
  AdvancedFormTranslations
} from "@components/form/advanced";
import {PokemonSerieDTO} from "@/types";
import {useVModel} from "@vueuse/core";
import {localizationCodes} from "@/localization";

interface Props {
  modelValue: PokemonSerieDTO;
}

interface Emits {
  (e: 'update:modelValue', value: PokemonSerieDTO): void;
  (e: 'save'): void;
}

const props = defineProps<Props>();
const emit = defineEmits<Emits>();
const value = useVModel(props, 'modelValue', emit);

</script>

<style lang="scss" scoped>
// styles for disabled/incompatible fields
:deep(.opacity-50) {
  opacity: 0.5;

  .form-control {
    background-color: #f8f9fa;
    cursor: not-allowed;
  }
}
</style>