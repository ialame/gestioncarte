<template>
  <div class="d-flex flex-row overflow-hidden">
    <div class="set-icon">
      <OptionalImage v-if="computedSet?.shortName" :src="getSetIconLink(computedSet?.shortName)" />
    </div>
    <Flag v-if="actualLangue" class="mt-2 me-1" :lang="actualLangue" />
    <template v-if="computedShowParent">
      {{ parentName }}
      <IdTooltip :id="computedSet?.parentId" />
      &nbsp;|&nbsp;
    </template>
    {{ name }}
    <IdTooltip :id="computedSet?.id" />
  </div>
</template>

<script lang="ts" setup>
import {PokemonSetDTO} from "@/types";
import {computed} from "vue";
import IdTooltip from "@components/tooltip/IdTooltip.vue";
import OptionalImage from "@components/OptionalImage.vue";
import {Flag, LocalizationCode} from "@/localization";
import {computedAsync} from "@vueuse/core";
import {PokemonComposables} from "@/vue/composables/pokemon/PokemonComposables";
import {getSetName} from "@components/cards/pokemon/set/logic";
import {getSetIconLink} from "@components/cards/pokemon/set/icon";
import pokemonSetService = PokemonComposables.pokemonSetService;

interface Props {
  set?: PokemonSetDTO;
  showParent?: boolean;
  langue?: LocalizationCode;
}

const props = withDefaults(defineProps<Props>(), {
  showParent: () => false,
});

const computedSet = computedAsync<PokemonSetDTO | undefined>(() => props.set);

const name = computed(() => {
  if (props.langue && props.set?.translations?.[props.langue]?.name) {
    return props.set.translations[props.langue]!.name;
  }

  const translations = props.set?.translations;
  if (translations) {
    const availableTranslation = Object.values(translations).find(t => t?.name);
    if (availableTranslation?.name) {
      return availableTranslation.name;
    }
  }

  return props.set?.shortName || 'Unknown Set';
});

const actualLangue = computed(() => {
  if (props.langue && props.set?.translations?.[props.langue]?.name) {
    return props.langue;
  }

  const translations = props.set?.translations;
  if (translations) {
    const availableLang = Object.keys(translations).find(lang =>
        translations[lang as LocalizationCode]?.name
    );
    return (availableLang as LocalizationCode) || props.langue;
  }

  return props.langue;
});

const computedShowParent = computed(() => props.showParent && computedSet.value?.parentId);
const parentName = computedAsync(async () => computedShowParent.value ? getSetName(await pokemonSetService.get(computedSet.value?.parentId as string)) : '', '');
</script>

<style lang="scss" scoped>
.set-icon {
  width: 30px;
  margin-right: 0.5rem;
  text-align: center;

  :deep(object) {
    height: 16px;
  }
}
</style>