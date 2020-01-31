const Users = (function () {
    const USER_ID = 'bbop_user_id';
    const USER_NAME = 'bbop_user_name';

    class UserClass {

        constructor(gameConfig, id, name) {
            this.gameConfig = gameConfig;
            this._id = id;
            this._name = name;
        }

        /**
         * @deprecated use id()
         * @returns {*}
         */
        get() {
            return this._id;
        }

        id() {
            return this._id;
        }

        hasName() {
            return !!this._name;
        }

        name() {
            return this._name || this._id.substr(0, this.gameConfig.hud.nameLength);
        }

        setName(n) {
            if (n) {
                this._name = n;
                document.cookie = USER_NAME + "=" + this._name;
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
            this.sessionUser = false;
        }

        fromSession() {
            if (!this.sessionUser) {
                const userIdFromCookie = this.getCookie(USER_ID);
                const userNameFromCookie = this.getCookie(USER_NAME);
                const id = !userIdFromCookie ? this.genUuid() : userIdFromCookie;
                document.cookie = USER_ID + "=" + id;
                this.sessionUser = new UserClass(this.gameConfig, id, userNameFromCookie);
                console.log(this.sessionUser.id() + " " + this.sessionUser.name());
                this.users[this.sessionUser.id()] = this.sessionUser;
            }
            return this.sessionUser;
        }

        get(id) {
            if (!this.users[id]) {
                this.users[id] = new UserClass(this.gameConfig, id, null);
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