import RealmManager from './RealmManager';
var Promise = require('bluebird');

Promise.config({
    // Enable warnings
    warnings: true,
    // Enable long stack traces
    longStackTraces: true,
    // Enable cancellation
    cancellation: true,
    // Enable monitoring
    monitoring: true
});

export const DECK_MODE = 'deck-mode';
export const LIST_MODE = 'list-mode';
export const SettingsSchema = {
    name: 'Settings',
    primaryKey: 'id',
    properties: {
        id: 'int',
        selectorMode: {type: 'string', default: DECK_MODE}
    }
}

let instance = null;
let _db_result = null;

export default class Settings {

    static async get() {
        return new Promise(async function(resolve, reject) {
            if (!instance) {
                let loaded = await Settings.load();
                instance = new Settings(loaded);
            }
            resolve(instance);
        });
    }

    static async load() {
        return new Promise(async function (resolve, reject) {
            let saved = await RealmManager.getRealm().objects('Settings').filtered('id = 0');
            if(Object.keys(saved).length === 0 && saved.constructor === Object) {
                console.log('creating setting object...');
                let realm = RealmManager.getRealm(); 
                await realm.write(async () => {
                    await realm.create('Settings', {id: 0});
                });
            }
            console.log(saved);
            _db_result = saved[0];
            resolve(saved[0]);
        });
    }

    constructor(setupSettings) {
        if (!setupSettings) {
            this._setupError();
        }

        // Setup instance with object

        if (!setupSettings.selectorMode)
            this._setupError();
        
        this.selectorMode = setupSettings.selectorMode;
        
    }

    async save() {
        let settingsInstance = this;
        return new Promise(async function(resolve, reject) {
            let realm = RealmManager.getRealm();
            await realm.write(() => {
                _db_result.selectorMode = settingsInstance.selectorMode;
                resolve();
            });
        });
    }

    change(newSettings) {
        if (newSettings.selectorMode) {
            this.selectorMode = newSettings.selectorMode;
        }
        return this.save();
    }



    _setupError() { throw new Error('Undefined initial settings! Did you try to construct settings without the static get() call?'); }

}