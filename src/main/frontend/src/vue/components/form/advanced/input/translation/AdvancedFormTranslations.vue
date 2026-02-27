<template>
  <AdvancedFormCollapse class="advanced-form-translations card" :path="path" :label="label">
    <template #after-label>
      <template v-if="!readOnly">
        <FormButton v-if="reviewable" color="success" class="form-btn ms-2" @click.stop="reviewed = true">
          <Icon name="checkmark-outline" />
        </FormButton>
      </template>
      <AdvancedFormFeedback class="ms-2" :validationResults="validationResults" />
      <AdvancedFormMergeButton v-if="readOnly" :path="path" />
      <div v-if="!readOnly && !fixedLocalizations && missingLocalizations.length > 0" class="ms-auto mb-3 missing-localizations">
        <FormButton
            v-for="l in missingLocalizations"
            :key="l"
            color="link"
            class="p-0 no-focus me-1"
            @click="addLocalizationWithGroupCheck(l)"
            :disabled="!canAddLanguage(l)"
            :class="{ 'opacity-50': !canAddLanguage(l), 'cursor-not-allowed': !canAddLanguage(l) }"
            :title="!canAddLanguage(l) ? getGroupErrorMessage(l) : ''">
          <Flag :lang="l" />
          <span v-if="!canAddLanguage(l)" class="small text-danger ms-1">✗</span>
        </FormButton>
      </div>
    </template>
    <template #default="{required: r}">
      <div v-if="hasIncompatibleLanguages" class="alert alert-warning m-2 p-2 small">
        ⚠️ Warning: You have mixed incompatible language groups. Please remove languages from one group.
      </div>

      <template v-for="(l, i) in usedLocalizations" :key="l">
        <div v-if="!isLanguageCompatible(l)" class="text-danger small mb-1 ms-2">
          ⚠️ Incompatible language group
        </div>
        <component :is="translationComponent"
                   :path="concatPaths([path, l])"
                   :localization="l"
                   :required="r"
                   :availableSubpath="availableSubpath || ''"
                   :disableAvailability="!isLanguageAvailable(l)"
                   @remove="removeLocalization(l)"
                   #default="slotProps">

          <slot v-bind="{...slotProps, isLanguageCompatible: isLanguageCompatible(l) }" />
          <template v-if="i !== usedLocalizations.length - 1">
            <slot name="separator"><hr class="card-separator" /></slot>
          </template>
        </component>
      </template>
    </template>
  </AdvancedFormCollapse>
</template>

<script lang="ts" setup>
import {computed} from 'vue';
import AdvancedFormCollapse from "@components/form/advanced/AdvancedFormCollapse.vue";
import AdvancedFormMergeButton from "@components/form/advanced/merge/AdvancedFormMergeButton.vue";
import AdvancedFormFeedback from "@components/form/advanced/AdvancedFormFeedback.vue";
import {useAdvancedFormInput, watchAdvancedForm} from "@components/form/advanced/logic";
import {Flag, LocalizationCode, localizationCodes, sortLocalizations, Translations} from "@/localization";
import {cloneDeep, concat, get, set, uniq} from "lodash";
import FormButton from "@components/form/FormButton.vue";
import {concatPaths, Path} from "@/path";
import AdvancedFormTranslation from "@components/form/advanced/input/translation/AdvancedFormTranslation.vue";
import Icon from "@components/Icon.vue";
import {AsyncPredicate} from "@/types";
import {useProvideAdvancedFormTranslationContext} from "@components/form/advanced/input/translation/logic";
import AdvancedFormSingleLineTranslation
  from "@components/form/advanced/input/translation/AdvancedFormSingleLineTranslation.vue";


interface Props {
  path: Path;
  availableSubpath?: Path;
  localizationSubpath?: Path;
  localizations?: LocalizationCode[];
  label?: string;
  singleLine?: boolean;
  fixedLocalizations?: boolean;
  removeConfirmation?: AsyncPredicate<LocalizationCode>;
  enforceLanguageGroups?: boolean;
}

interface Emits {
  (e: 'update:localizations', args: LocalizationCode[]): void;
}

const props = withDefaults(defineProps<Props>(), {
  availableSubpath: 'available',
  localizationSubpath: 'localization',
  localizations: () => localizationCodes,
  label: 'Traductions',
  singleLine: false,
  fixedLocalizations: false,
  enforceLanguageGroups: false
});
const emit = defineEmits<Emits>();

const { value, source, readOnly, validationResults, reviewable, reviewed } = useAdvancedFormInput<any, Translations<any>>(() => props.path);
useProvideAdvancedFormTranslationContext(() => props.fixedLocalizations, () => props.removeConfirmation);

// Language group helper functions
const getLanguageGroup = (lang: LocalizationCode): string => {
  if (['us', 'fr', 'de', 'es', 'it', 'nl', 'pt', 'kr', 'ru'].includes(lang)) return 'west';
  if (lang === 'jp') return 'jp';
  if (lang === 'cn') return 'cn';
  if (lang === 'zh') return 'zh';
  return 'other';
};

// Check if a language has content (not just empty/whitespace)
// Update the hasContent function to be more versatile
const hasContent = (langOrTranslation: LocalizationCode | any): boolean => {
  let translation: any;

  if (typeof langOrTranslation === 'string') {
    // It's a language code
    const lang = langOrTranslation as LocalizationCode;
    if (!value.value || !value.value[lang]) return false;
    translation = value.value[lang];
  } else {
    // It's a translation object
    translation = langOrTranslation;
  }

  const contentFields = ['name'];

  return contentFields.some(field => {
    const fieldValue = get(translation, field, '');
    return fieldValue && fieldValue.toString().trim().length > 0;
  });
};

// Get languages that have actual content
const languagesWithContent = computed(() => {
  if (!value.value) return [];

  return Object.keys(value.value).filter(lang =>
      hasContent(lang as LocalizationCode)
  ) as LocalizationCode[];
});

// Get the primary content language (first one with content)
const primaryContentLanguage = computed(() => {
  if (languagesWithContent.value.length === 0) return null;
  return languagesWithContent.value[0];
});

// Get the group of the primary content language
const primaryContentGroup = computed(() => {
  if (!primaryContentLanguage.value) return null;
  return getLanguageGroup(primaryContentLanguage.value);
});

const hasIncompatibleLanguages = computed(() => {
  if (!props.enforceLanguageGroups || languagesWithContent.value.length <= 1) return false;

  const groups = new Set<string>();
  languagesWithContent.value.forEach(lang => {
    groups.add(getLanguageGroup(lang));
  });

  return groups.size > 1;
});

const isLanguageCompatible = (lang: LocalizationCode): boolean => {
  if (!props.enforceLanguageGroups || languagesWithContent.value.length <= 1) return true;

  const langGroup = getLanguageGroup(lang);

  return languagesWithContent.value.every(contentLang =>
      getLanguageGroup(contentLang) === langGroup
  );
};

const isLanguageAvailable = (lang: LocalizationCode): boolean => {
  if (!props.enforceLanguageGroups) return true;

  const contentLangs = languagesWithContent.value;

  if (contentLangs.length === 0) return true;

  const langGroup = getLanguageGroup(lang);
  const primaryGroup = primaryContentGroup.value;

  return langGroup === primaryGroup;
};

const canAddLanguage = (lang: LocalizationCode): boolean => {
  if (!props.enforceLanguageGroups) return true;

  const contentLangs = languagesWithContent.value;

  if (contentLangs.length === 0) return true;

  const newGroup = getLanguageGroup(lang);
  const primaryGroup = primaryContentGroup.value;

  return newGroup === primaryGroup;
};

const getGroupErrorMessage = (lang: LocalizationCode): string => {
  const contentLangs = languagesWithContent.value;
  if (contentLangs.length === 0) return '';

  const groups = new Set<string>();
  contentLangs.forEach(l => groups.add(getLanguageGroup(l)));
  const contentGroups = Array.from(groups);

  const newGroup = getLanguageGroup(lang);

  const groupNames: Record<string, string> = {
    'west': 'Western (EN, FR, DE, ES, IT, PT, NL, KR, RU)',
    'jp': 'Japanese',
    'cn': 'Traditional Chinese',
    'zh': 'Simplified Chinese',
  };

  if (contentGroups.length === 1) {
    return `Cannot mix ${groupNames[newGroup] || newGroup} with ${groupNames[contentGroups[0]] || contentGroups[0]}`;
  } else {
    return `Cannot add ${groupNames[newGroup] || newGroup} to mixed language groups`;
  }
};

const availableLocalizations = computed(() => {
  if (!source.value) {
    return [];
  }

  const localizations: LocalizationCode[] = [];

  for (const [k, v] of Object.entries(source.value)) {
    if (!props.availableSubpath || get(v, props.availableSubpath)) {
      localizations.push(k as LocalizationCode);
    }
  }
  return localizations;
});

const newlyAddedLocalizations = new Set<LocalizationCode>();

const addLocalization = (localization: LocalizationCode) => {
  if (!value.value) {
    value.value = {};
    return;
  }

  const copy = cloneDeep(value.value);

  if (!copy[localization]) {
    copy[localization] = {};
    newlyAddedLocalizations.add(localization);
  }

  value.value = copy;
  emit('update:localizations', uniq([...props.localizations, localization]));
};

const addLocalizationWithGroupCheck = (localization: LocalizationCode) => {
  if (!canAddLanguage(localization)) {
    const contentLangs = languagesWithContent.value;
    const groups = new Set<string>();
    contentLangs.forEach(l => groups.add(getLanguageGroup(l)));
    const contentGroups = Array.from(groups);

    const newGroup = getLanguageGroup(localization);

    const groupNames: Record<string, string> = {
      'west': 'Western',
      'jp': 'Japanese',
      'cn': 'Traditional Chinese',
      'zh': 'Simplified Chinese',
    };

    const currentGroupName = contentGroups.length === 1
        ? groupNames[contentGroups[0]] || contentGroups[0]
        : 'mixed groups';

    const response = confirm(
        `You're trying to add a ${groupNames[newGroup] || newGroup} language to ${currentGroupName}.\n\n` +
        `This will remove all existing content.\n` +
        `Do you want to continue?`
    );

    if (response) {
      // Clear all existing translations
      value.value = {};
      // Add the new language
      addLocalization(localization);
    }
    return;
  }

  addLocalization(localization);
};

const removeLocalization = (localization: LocalizationCode) => {
  if (!value.value) {
    return;
  }

  const copy = cloneDeep(value.value);

  // Always delete the entire translation when removing via button
  delete copy[localization];

  value.value = copy;
  emit('update:localizations', props.localizations.filter(l => l !== localization));
};

const usedLocalizations = computed(() => sortLocalizations(uniq(readOnly.value ? availableLocalizations.value : concat(props.localizations, availableLocalizations.value))));
const missingLocalizations = computed(() => sortLocalizations(localizationCodes.filter(l => !usedLocalizations.value.includes(l))));

const translationComponent = computed(() => props.singleLine ? AdvancedFormSingleLineTranslation : AdvancedFormTranslation);

watchAdvancedForm<any>((data: any) => {
  const copy = get(data, props.path);

  if (!copy) {
    set(data, props.path, {});
    return;
  }

  // Track if we need to clean up
  const languagesToRemove: LocalizationCode[] = [];

  for (const k of Object.keys(copy)) {
    const l = k as LocalizationCode;

    if (copy[l]) {
      // Check content BEFORE any modifications
      const translationHasContent = hasContent(copy[l]);

      // Skip removal check for localizations that are in props.localizations
      // or for newly added localizations (to prevent race condition with watchAdvancedForm)
      if (!translationHasContent && !props.localizations.includes(l) && !newlyAddedLocalizations.has(l)) {
        languagesToRemove.push(l);
        continue;
      }

      // Only process languages that have content
      if (props.availableSubpath) {
        set(copy[l], props.availableSubpath, true);
      }

      set(copy[l], 'code', l);

      if (props.localizationSubpath && props.localizationSubpath !== 'code') {
        set(copy[l], props.localizationSubpath, l);
      }
    }
  }

  // Clear the newly added set after processing, but save a copy first to protect all newly added languages
  const newlyAddedLocalizationsSnapshot = Array.from(newlyAddedLocalizations);
  newlyAddedLocalizations.clear();

  // Remove marked languages
  languagesToRemove.forEach(lang => {
    delete copy[lang];
  });

  // Update the data
  set(data, props.path, copy);

  // Check if any languages were just added (use snapshot to include all languages added in this batch)
  // This prevents them from being removed due to having no content yet
  newlyAddedLocalizationsSnapshot.forEach(lang => {
    newlyAddedLocalizations.add(lang);
  });
});

</script>

<style lang="scss" scoped>
@import "../../AdvancedForm";

.advanced-form-translations {
  :deep(>div>.form-label) {
    @include bold-label;
  }
  .missing-localizations {
    background: $light-bg;
    padding: 0.1rem 0.3rem 0.4rem;

    .cursor-not-allowed {
      cursor: not-allowed;
    }

    .opacity-50 {
      opacity: 0.5;
    }
  }

  :deep(.alert-warning) {
    background-color: #fff3cd;
    border-color: #ffeaa7;
    color: #856404;
    font-size: 0.875rem;
    margin: 0.5rem;
    border-radius: 0.25rem;
  }
}
</style>