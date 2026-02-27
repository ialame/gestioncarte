import { ref } from 'vue';
import { LocalizationCode } from '@/localization';

const STORAGE_KEY = 'pokemon_extraction_localization_state';

function loadFromStorage() {
    try {
        const raw = sessionStorage.getItem(STORAGE_KEY);
        return raw ? JSON.parse(raw) : null;
    } catch {
        return null;
    }
}

function saveToStorage(
    filter: LocalizationCode | undefined,
    localizations: LocalizationCode[],
    setIds: Partial<Record<LocalizationCode, string[]>>
) {
    try {
        sessionStorage.setItem(STORAGE_KEY, JSON.stringify({ filter, localizations, setIds }));
    } catch { /* ignore storage errors */ }
}

function clearStorage() {
    try {
        sessionStorage.removeItem(STORAGE_KEY);
    } catch { /* ignore */ }
}

const _stored = loadFromStorage();

// The language currently selected in PokemonSetSearch (most recently extracted)
const extractionLocalizationFilter = ref<LocalizationCode | undefined>(_stored?.filter ?? undefined);

// Accumulated list of all languages that have been explicitly extracted in this session.
const extractedLocalizations = ref<LocalizationCode[]>(_stored?.localizations ?? []);

// Tracks exactly which set IDs were extracted per language.
// e.g. { us: ['phantasmal-us', 'base1'], jp: ['svp-jp'] }
const extractedSetIdsByLocalization = ref<Partial<Record<LocalizationCode, string[]>>>(_stored?.setIds ?? {});

/**
 * Composable to share the extraction localization filter between components.
 * State is persisted in sessionStorage so page refreshes don't lose it.
 */
export function useExtractionLocalizationFilter() {
    const setExtractionLocalization = (localization: LocalizationCode | undefined) => {
        extractionLocalizationFilter.value = localization;
        if (localization && !extractedLocalizations.value.includes(localization)) {
            extractedLocalizations.value = [...extractedLocalizations.value, localization];
        }
        saveToStorage(extractionLocalizationFilter.value, extractedLocalizations.value, extractedSetIdsByLocalization.value);
    };

    const clearExtractionLocalization = () => {
        extractionLocalizationFilter.value = undefined;
        saveToStorage(undefined, extractedLocalizations.value, extractedSetIdsByLocalization.value);
    };

    // Call this right after starting an extraction to record the set ID for the current language
    const addExtractedSetId = (setId: string) => {
        const loc = extractionLocalizationFilter.value;
        if (!loc) return;
        const current = extractedSetIdsByLocalization.value[loc] ?? [];
        if (!current.includes(setId)) {
            extractedSetIdsByLocalization.value = {
                ...extractedSetIdsByLocalization.value,
                [loc]: [...current, setId]
            };
            saveToStorage(extractionLocalizationFilter.value, extractedLocalizations.value, extractedSetIdsByLocalization.value);
        }
    };

    // Call when all extracted cards are saved or cleared — resets the full session
    const resetExtractedLocalizations = () => {
        extractedLocalizations.value = [];
        extractionLocalizationFilter.value = undefined;
        extractedSetIdsByLocalization.value = {};
        clearStorage();
    };

    return {
        extractionLocalizationFilter,
        extractedLocalizations,
        extractedSetIdsByLocalization,
        setExtractionLocalization,
        clearExtractionLocalization,
        addExtractedSetId,
        resetExtractedLocalizations
    };
}
