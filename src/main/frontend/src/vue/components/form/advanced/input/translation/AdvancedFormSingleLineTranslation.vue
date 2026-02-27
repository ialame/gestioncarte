<template>
  <div class="advanced-form-translation mb-3" v-bind="columnProps">
    <div class="d-flex flex-row w-100" :class="statusClass">
      <Flag class="icon-24 me-2 mt-1" :lang="localization" />
      <slot
          :localization="localization"
          :required="required"
          :path="path"
          :disableAvailability="disableAvailability"
          :updateContent="updateContent"
      />
      <AdvancedFormRemoveTranslationButton :path="path" :localization="localization" @remove="emit('remove')" />
      <FormButton v-if="reviewable" color="success" class="form-btn ms-2" @click="reviewed = true">
        <Icon name="checkmark-outline" />
      </FormButton>
      <AdvancedFormMergeButton :path="path" />
    </div>
    <AdvancedFormFeedback class="mt-2" :validationResults="validationResults" />
  </div>
</template>

<script lang="ts" setup>
import AdvancedFormMergeButton from "@components/form/advanced/merge/AdvancedFormMergeButton.vue";
import AdvancedFormFeedback from "@components/form/advanced/AdvancedFormFeedback.vue";
import {useAdvancedFormInput} from "@components/form/advanced/logic";
import {LocalizationCode} from "@/localization";
import FormButton from "@components/form/FormButton.vue";
import Icon from "@components/Icon.vue";
import {Path, pathToString} from "@/path";
import Flag from "@/localization/Flag.vue";
import AdvancedFormRemoveTranslationButton
  from "@components/form/advanced/input/translation/AdvancedFormRemoveTranslationButton.vue";
import {useAlignedElement} from "@components/form/advanced/merge/alignment";
import {computed, watch} from 'vue';
import {get} from 'lodash';

interface Props {
  path: Path;
  localization: LocalizationCode;
  required?: boolean;
  availableSubpath?: string;
  disableAvailability?: boolean;
}
interface Emits {
  (e: 'remove'): void;
  (e: 'content-change', hasContent: boolean): void; // Emit content changes
}

const props = withDefaults(defineProps<Props>(), {
  required: false,
  availableSubpath: '',
  disableAvailability: false,
});
const emit = defineEmits<Emits>();

const columnProps = useAlignedElement(() => pathToString(props.path));
const { value, validationResults, statusClass, reviewable, reviewed } = useAdvancedFormInput<any, any>(() => props.path);

// Check if this translation has content
const hasContent = computed(() => {
  if (!value.value) return false;

  const contentFields = ['name'];

  return contentFields.some(field => {
    const fieldValue = get(value.value, field, '');
    return fieldValue && fieldValue.toString().trim().length > 0;
  });
});

// Function to manually trigger content update
const updateContent = (hasContent: boolean) => {
  emit('content-change', hasContent);
};

// Watch for content changes in the value
watch(() => value.value, (newVal) => {
  if (!newVal) {
    emit('content-change', false);
    return;
  }

  const contentFields = ['name'];
  const hasAnyContent = contentFields.some(field => {
    const fieldValue = get(newVal, field, '');
    return fieldValue && fieldValue.toString().trim().length > 0;
  });

  emit('content-change', hasAnyContent);
}, { deep: true });

// Also emit initial content state
watch(() => hasContent.value, (newHasContent) => {
  emit('content-change', newHasContent);
}, { immediate: true });

</script>