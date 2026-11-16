
const IocI18n = (() => {
    const STORAGE_KEY = 'ioc-visualizer.language';
    const NAMES = { en: 'English', ru: 'Русский' };

    const dictionaries = {
        en: {
            'page.title': 'Bean graph',
            'app.title': 'Bean graph',
            'lang.switch': 'Switch the interface to Russian',

            'search.label': 'Find a bean',
            'search.placeholder': 'Find a bean by name',
            'search.submit': 'Find',
            'search.none': 'No bean matching “{query}” in this view.',
            'search.several': '{count} matches, showing {name}.',
            'search.next': '{index} of {count} · Tab for next, Shift+Tab for previous',

            'filters.title': 'Filters',
            'filters.reset': 'Reset filters',
            'filter.system': 'Only application beans',
            'filter.solo': 'Hide beans without links',
            'filter.serviceLinks': 'Show “declared in” and @DependsOn links',
            'filter.package': 'Package starts with',
            'filter.scope': 'Scope',
            'filter.scope.any': 'Any',
            'filter.stereotype': 'Stereotype',
            'filter.stereotype.any': 'Any',
            'filter.stereotype.hint': 'Beans of this stereotype and the beans they use.',

            'analysis.title': 'Analysis',
            'legend.title': 'Legend',
            'legend.other': 'Other application bean',
            'legend.injected': 'Injected dependency',
            'legend.declared': 'Declared in, @DependsOn',

            'aria.controls': 'Graph controls',
            'aria.graph': 'Dependency graph',
            'aria.details': 'Bean details',
            'aria.closeDetails': 'Close details',
            'aria.closeScheme': 'Close the cycle scheme',
            'graph.fit': 'Fit graph',
            'focus.label': 'Module cycle',
            'focus.showInGraph': 'Show in full graph',

            'status.loading': 'Loading graph…',
            'status.notLoaded': 'Graph not loaded',
            'status.layout': 'Laying out the graph…',
            'graph.empty': 'No beans match these filters.',
            'error.notFound': 'The graph API is not available. Check that the application sets ioc-visualizer.enabled=true.',
            'error.badFilters': 'The server rejected the filters: {detail}.',
            'error.badRequest': 'bad request',
            'error.unreachable': 'Can’t reach the application. Check that it is running.',
            'error.http': 'The server returned an error (HTTP {status}).',

            'count.beans': { one: '{count} bean', other: '{count} beans' },
            'count.links': { one: '{count} link', other: '{count} links' },
            'count.cycles': { one: '{count} cycle', other: '{count} cycles' },

            'stereotype.CONTROLLER': 'Controller',
            'stereotype.SERVICE': 'Service',
            'stereotype.REPOSITORY': 'Repository',
            'stereotype.CONFIGURATION': 'Configuration',
            'stereotype.UNKNOWN': 'No stereotype',
            'scope.SINGLETON': 'Singleton',
            'scope.PROTOTYPE': 'Prototype',
            'scope.unknown': 'Unknown scope',
            'bean.application': 'Application bean',
            'bean.framework': 'Framework or library bean',
            'bean.unknownClass': 'Unknown class',

            'links.dependsOn': 'Depends on',
            'links.usedBy': 'Used by',
            'links.noDependencies': 'Doesn’t depend on other beans in this view.',
            'links.noDependents': 'No bean in this view depends on it.',
            'links.direction': 'Direction of links',
            'link.via': ' · via {type}',
            'link.CONSTRUCTOR': 'constructor',
            'link.FIELD': 'field',
            'link.SETTER': 'method',
            'link.FACTORY_METHOD': '@Bean method',
            'link.UNKNOWN': 'unknown',
            'link.DECLARED_IN': 'declared in',
            'link.DEPENDS_ON': '@DependsOn',

            'cycles.loading': 'Looking for cycles…',
            'cycles.failed': 'Cycles not loaded: the server returned HTTP {status}.',
            'cycles.none': 'No cycles',
            'cycles.noneFound': 'No cycles found.',
            'cycles.betweenModules': 'Between modules',
            'cycles.betweenBeans': 'Between beans',
            'cycles.moduleHint': 'Usually the direction with the fewest links is the one to remove.',
            'cycles.fewest': 'мин.',
            'cycles.andMore': '{names} and {count} more',
            'cycles.hidden': 'hidden by filters',
            'cycles.shown': '{shown} of {total} shown',

            'scheme.status': 'Module cycle: {beans}, {links}',
            'scheme.declaredIn': 'its @Bean method is declared in {module}',
            'scheme.injectedAs': 'injected as {type}',
            'scheme.shownIn': 'Shown in {module}: {reasons}'
        },

        ru: {
            'page.title': 'Граф бинов',
            'app.title': 'Граф бинов',
            'lang.switch': 'Переключить интерфейс на английский',

            'search.label': 'Найти бин',
            'search.placeholder': 'Имя бина',
            'search.submit': 'Найти',
            'search.none': 'В этом виде нет бина, подходящего под «{query}».',
            'search.several': 'Совпадений: {count}, показан {name}.',
            'search.next': '{index} из {count} · Tab — следующий, Shift+Tab — предыдущий',

            'filters.title': 'Фильтры',
            'filters.reset': 'Сбросить фильтры',
            'filter.system': 'Только бины приложения',
            'filter.solo': 'Скрыть бины без связей',
            // неразрывный пробел: «в» не должно оставаться одно на следующей строке
            'filter.serviceLinks': 'Показывать связи «объявлен\u00A0в» и @DependsOn',
            'filter.package': 'Пакет начинается с',
            'filter.scope': 'Область видимости',
            'filter.scope.any': 'Любая',
            'filter.stereotype': 'Стереотип',
            'filter.stereotype.any': 'Любой',
            'filter.stereotype.hint': 'Бины с этим стереотипом и бины, которые они используют.',

            'analysis.title': 'Анализ',
            'legend.title': 'Легенда',
            'legend.other': 'Другой бин приложения',
            'legend.injected': 'Внедрённая зависимость',
            'legend.declared': 'Объявлен в, @DependsOn',

            'aria.controls': 'Управление графом',
            'aria.graph': 'Граф зависимостей',
            'aria.details': 'Сведения о бине',
            'aria.closeDetails': 'Закрыть сведения',
            'aria.closeScheme': 'Закрыть схему цикла',
            'graph.fit': 'Вписать граф',
            'focus.label': 'Цикл модулей',
            'focus.showInGraph': 'Показать в полном графе',

            'status.loading': 'Загрузка графа…',
            'status.notLoaded': 'Граф не загружен',
            'status.layout': 'Раскладка графа…',
            'graph.empty': 'Нет бинов, подходящих под фильтры.',
            'error.notFound': 'API графа недоступно. Проверьте, что в приложении задано ioc-visualizer.enabled=true.',
            'error.badFilters': 'Сервер отклонил фильтры: {detail}.',
            'error.badRequest': 'неверный запрос',
            'error.unreachable': 'Не удаётся связаться с приложением. Проверьте, что оно запущено.',
            'error.http': 'Сервер вернул ошибку (HTTP {status}).',

            'count.beans': { one: '{count} бин', few: '{count} бина', many: '{count} бинов', other: '{count} бина' },
            'count.links': { one: '{count} связь', few: '{count} связи', many: '{count} связей', other: '{count} связи' },
            'count.cycles': { one: '{count} цикл', few: '{count} цикла', many: '{count} циклов', other: '{count} цикла' },

            'stereotype.CONTROLLER': 'Контроллер',
            'stereotype.SERVICE': 'Сервис',
            'stereotype.REPOSITORY': 'Репозиторий',
            'stereotype.CONFIGURATION': 'Конфигурация',
            'stereotype.UNKNOWN': 'Без стереотипа',
            'scope.SINGLETON': 'Singleton',
            'scope.PROTOTYPE': 'Prototype',
            'scope.unknown': 'Область не определена',
            'bean.application': 'Бин приложения',
            'bean.framework': 'Бин фреймворка или библиотеки',
            'bean.unknownClass': 'Класс не определён',

            'links.dependsOn': 'Зависит от',
            'links.usedBy': 'Используется в',
            'links.noDependencies': 'Не зависит от других бинов в этом виде.',
            'links.noDependents': 'В этом виде от него не зависит ни один бин.',
            'links.direction': 'Направление связей',
            'link.via': ' · через {type}',
            'link.CONSTRUCTOR': 'конструктор',
            'link.FIELD': 'поле',
            'link.SETTER': 'метод',
            'link.FACTORY_METHOD': '@Bean-метод',
            'link.UNKNOWN': 'неизвестно',
            'link.DECLARED_IN': 'объявлен в',
            'link.DEPENDS_ON': '@DependsOn',

            'cycles.loading': 'Поиск циклов…',
            'cycles.failed': 'Циклы не загружены: сервер вернул HTTP {status}.',
            'cycles.none': 'Циклов нет',
            'cycles.noneFound': 'Циклы не найдены.',
            'cycles.betweenModules': 'Между модулями',
            'cycles.betweenBeans': 'Между бинами',
            'cycles.moduleHint': 'Обычно разрывают то направление, где связей меньше всего.',
            'cycles.fewest': 'мин.',
            'cycles.andMore': '{names} и ещё {count}',
            'cycles.hidden': 'скрыто фильтрами',
            'cycles.shown': 'показано {shown} из {total}',

            'scheme.status': 'Цикл модулей: {beans}, {links}',
            'scheme.declaredIn': 'его @Bean-метод объявлен в {module}',
            'scheme.injectedAs': 'внедрён как {type}',
            'scheme.shownIn': 'Показан в {module}: {reasons}'
        }
    };

    const languages = Object.keys(dictionaries);

    // localStorage бывает недоступен (запрет в настройках браузера): тогда язык не запомнить,
    // а кнопка, которая после перезагрузки ничего не меняет, только путает, поэтому её прячут
    function readStored() {
        try {
            return localStorage.getItem(STORAGE_KEY);
        } catch (error) {
            return null;
        }
    }

    function canStore() {
        try {
            localStorage.setItem(STORAGE_KEY + '.probe', '1');
            localStorage.removeItem(STORAGE_KEY + '.probe');
            return true;
        } catch (error) {
            return false;
        }
    }

    // Сохранённый выбор, иначе первый из языков браузера, который у нас есть, иначе английский
    function detect() {
        const stored = readStored();
        if (stored && dictionaries[stored]) return stored;
        const preferred = navigator.languages && navigator.languages.length ? navigator.languages : [navigator.language];
        for (const tag of preferred) {
            const base = String(tag).toLowerCase().split('-')[0];
            if (dictionaries[base]) return base;
        }
        return 'en';
    }

    const language = detect();
    const rules = new Intl.PluralRules(language);

    function has(key) {
        return Object.prototype.hasOwnProperty.call(dictionaries[language], key);
    }

    function format(template, params) {
        return template.replace(/\{(\w+)\}/g, (placeholder, name) => (name in params ? String(params[name]) : placeholder));
    }

    // Нет ключа в текущем словаре — берётся английский, нет и там — показывается сам ключ,
    // чтобы пропуск был виден на экране
    function t(key, params = {}) {
        const entry = has(key) ? dictionaries[language][key] : dictionaries.en[key];
        if (entry === undefined) return key;
        const template = typeof entry === 'string' ? entry : (entry[rules.select(params.count)] || entry.other);
        return format(template, params);
    }

    function nextLanguage() {
        return languages[(languages.indexOf(language) + 1) % languages.length];
    }

    // Переводит разметку. Вызывается один раз при загрузке страницы, до первой отрисовки
    function apply() {
        document.documentElement.lang = language;
        document.title = t('page.title');
        document.querySelectorAll('[data-i18n]').forEach(node => {
            node.textContent = t(node.dataset.i18n);
        });
        ['placeholder', 'aria-label', 'title'].forEach(attribute => {
            document.querySelectorAll('[data-i18n-' + attribute + ']').forEach(node => {
                node.setAttribute(attribute, t(node.getAttribute('data-i18n-' + attribute)));
            });
        });

        // Кнопка называет язык, на который переключит, и на нём же: в русском интерфейсе «English», в английском «Русский»
        const button = document.getElementById('languageBtn');
        if (button) {
            const next = nextLanguage();
            button.textContent = NAMES[next];
            button.lang = next;
            button.title = t('lang.switch');
            button.hidden = !canStore();
        }
    }

    function switchLanguage() {
        try {
            localStorage.setItem(STORAGE_KEY, nextLanguage());
        } catch (error) {
            return;
        }
        location.reload();
    }

    return { t, has, apply, switchLanguage, language, dictionaries };
})();

const t = IocI18n.t;
