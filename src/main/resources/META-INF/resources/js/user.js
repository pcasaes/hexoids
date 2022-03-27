const Users = (function () {
    const USER_ID = 'hexoids_user_id';
    const USER_NAME = 'hexoids_user_name';

    function deriveName(guid, nameLength) {
        let id = "";
        for (let i = 0; i < guid.length && id.length < nameLength; i++) {
            id += guid[i];
        }
        return id.substr(0, nameLength)
    }

    class UserClass {

        constructor(gameConfig, id, idStr, name, sessionUser) {
            this.gameConfig = gameConfig;
            this._id = id;
            this._idStr = idStr;
            this._name = name;
            this.sessionUser = sessionUser;
        }

        /**
         * @deprecated use id()
         * @returns {*}
         */
        get() {
            return this._id;
        }

        idStr() {
            return this._idStr;
        }

        id() {
            return this._id;
        }

        hasName() {
            return !!this._name;
        }

        name() {
            return this._name || deriveName(this._id, this.gameConfig.hud.nameLength)
        }

        setName(n) {
            if (n) {
                this._name = n;
                if (this.sessionUser) {
                    document.cookie = USER_NAME + "=" + this._name;
                }
            } else {
                this._name = '';
            }
        }
    }

    class UsersClass {
        constructor(gameConfig, getCookie, genUuid) {
            this.gameConfig = gameConfig;
            this.getCookie = getCookie;
            this.genUuid = genUuid;
            this.users = {};
            this.sessionUser = null;
        }

        fromSession() {
            if (!this.sessionUser) {
                const userIdFromCookie = this.getCookie(USER_ID);
                const userNameFromCookie = this.getCookie(USER_NAME);
                const id = !userIdFromCookie ? this.genUuid() : this.genUuid(userIdFromCookie);
                document.cookie = USER_ID + "=" + id.str;
                this.sessionUser = new UserClass(this.gameConfig, id.id, id.str, userNameFromCookie, true);
                console.log(this.sessionUser.id() + " " + this.sessionUser.name());
                this.users[this.sessionUser.id()] = this.sessionUser;
            }
            return this.sessionUser;
        }

        get(id, idStr) {
            if (!this.users[id]) {
                this.users[id] = new UserClass(this.gameConfig, id, idStr, null, false);
            }
            return this.users[id];
        }
    }

    let instance;

    return {
        'get': (gameConfig, getCookie, genUuid) => {
            if (!instance) {
                instance = new UsersClass(gameConfig, getCookie, genUuid);
            }
            return instance;
        }
    };
})();


try {
    module.exports = Users;
} catch (ex) {
    console.debug("Could not export module. Only needed in nodejs. " + ex);
}