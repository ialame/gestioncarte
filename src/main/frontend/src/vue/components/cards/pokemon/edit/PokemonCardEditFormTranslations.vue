<template>
  <AdvancedFormTranslations path="translations" #default="{ path, localization }" :localizations="allLocalizations" :removeConfirmation="removeConfirmation" @update:localizations="onUpdateLocalizations">
    <FormRow v-if="!localizationsBoundToUS.includes(localization) || !value.translations.us?.available">
      <PokemonCardEditFormTranslationInput :path="concatPaths([path, 'number'])" :class="{'bold-input': localization === 'us' || localization === 'jp'}" :localization="localization" :sourceTranslations="sourceTranslations" label="Numéro" />
      <PokemonCardEditFormTranslationInput :path="concatPaths([path, 'rarity'])" :localization="localization" :sourceTranslations="sourceTranslations" label="Rareté" />
    </FormRow>
    <FormRow>
      <PokemonCardEditFormTranslationInput :path="concatPaths([path, 'name'])" :class="{'bold-input': localization === 'us' || localization === 'jp'}" :localization="localization" :sourceTranslations="sourceTranslations" label="Nom" />
      <PokemonCardEditFormTranslationInput :path="concatPaths([path, 'labelName'])" :localization="localization" :sourceTranslations="sourceTranslations" label="Nom d'étiquette"  />
    </FormRow>
    <FormRow v-if="localizations[localization].hasOriginalName && localization !== 'zh' && localization !== 'cn'">
      <PokemonCardEditFormTranslationInput :path="concatPaths([path, 'originalName'])" :localization="localization" :sourceTranslations="sourceTranslations" label="Nom en caractère kanji (日本語)"  />
    </FormRow>
    <FormRow v-if="value.type === 'Su' || value.type === 'Supporter' || value.translations[localization]?.trainer">
      <PokemonCardEditFormTranslationInput :path="concatPaths([path, 'trainer'])" :localization="localization" :sourceTranslations="sourceTranslations" label="Trainer"  />
    </FormRow>
    <template v-if="localizationsWithSet.includes(localization)">
      <FormRow>
        <AdvancedFormListInput :path="concatPaths([localization, 'brackets'])" label="Crochets" #default="{ path: bracketPath }" :initialValue="{localization: localization}">
          <BracketInput :path="bracketPath" :localization="localization" />
        </AdvancedFormListInput>
      </FormRow>
      <PromoList :localization="localization" path="promos" tcg="pokemon" />
      <FormRow>
        <PokemonSetSelect :path="concatPaths([localization, 'set'])" :values="sets.filter(s => !!s.translations?.[localization]?.available)" :langue="localization" advanced />
      </FormRow>
    </template>
  </AdvancedFormTranslations>
</template>

<script lang="ts" setup>
import FormRow from "@components/form/FormRow.vue";
import {PokemonComposables} from "@/vue/composables/pokemon/PokemonComposables";
import {AdvancedFormListInput, AdvancedFormTranslations, useAdvancedFormContext} from "@components/form/advanced";
import {EditedPokemonCard, localizationsBoundToUS, localizationsWithSet} from "@components/cards/pokemon/edit";
import PokemonCardEditFormTranslationInput
  from "@components/cards/pokemon/edit/PokemonCardEditFormTranslationInput.vue";
import {CardCertificationDTO, SourcedPokemonCardTranslationDTO} from "@/types";
import {LocalizationCode, localizations} from '@/localization';
import {BracketInput} from "@components/cards/pokemon/bracket";
import {concatPaths, Path} from "@/path";
import {computed, ref} from 'vue';
import {computedAsync} from "@vueuse/core";
import {getSetLocalizations, PokemonSetSelect} from "@components/cards/pokemon/set";
import rest from "@/rest";
import {ConfirmComposables} from "@/vue/composables/ConfirmComposables";
import {isEmpty, uniq} from "lodash";
import {h} from "vue";
import {PromoList} from "@components/cards/promo";
import confirmOrCancel = ConfirmComposables.confirmOrCancel;

interface Props {
  sourceTranslations: SourcedPokemonCardTranslationDTO[];
}

withDefaults(defineProps<Props>(), {
  sourceTranslations: () => []
});

const sets = PokemonComposables.pokemonSetService.all;

const { data: value } = useAdvancedFormContext<EditedPokemonCard>();

// Track manually added localizations beyond what setLocalizations provides
const manuallyAddedLocalizations = ref<LocalizationCode[]>([]);

const onUpdateLocalizations = (newLocalizations: LocalizationCode[]) => {
  // Update manuallyAddedLocalizations to include any new localizations
  manuallyAddedLocalizations.value = uniq([...manuallyAddedLocalizations.value, ...newLocalizations]);
};

// Combine setLocalizations with manually added localizations
const allLocalizations = computed(() => {
  const setLocs = setLocalizations.value || [];
  return uniq([...setLocs, ...manuallyAddedLocalizations.value]);
});

const getCertificationMessage = (localization: LocalizationCode, certifications: string[]) => h('div', {class: 'container'}, [
  h('span', {class: 'row'}, `Attention ! Il existe les certifications suivantes en ${localizations[localization].name} :`),
  h('ul', {class: 'row list-unstyled'}, certifications.map(c => h('li', {class: 'col'}, c))),
  h('span', {class: 'row'}, `Voulez-vous vraiment supprimer la traduction ?`)
]);
const removeConfirmation = async (localization: LocalizationCode) => {
  const certifications = (await rest.get(`/api/cards/${value.value.id}/certifications`) as CardCertificationDTO[])
      .filter(c => c.localization === localization)
      .map(c => c.barcode)
      .filter(c => !!c) as string[];

  if (!isEmpty(certifications)) {
    return confirmOrCancel(getCertificationMessage(localization, certifications));
  }
  return true;
};


const setLocalizations = computedAsync(() => getSetLocalizations([value.value.us?.set?.id, value.value.jp?.set?.id].filter(s => !!s) as string[]), []);
</script>


<style lang="scss" scoped>
.bold-input:deep(input.form-control) {
  font-weight: 700 !important;
}
</style>
