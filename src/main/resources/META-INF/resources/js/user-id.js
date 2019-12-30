class UserId {
    constructor(getCookie, genUuid) {
        this.getCookie = getCookie;
        this.genUuid = genUuid;
        this.id = null;
    }

    start() {
        const userIdFromCookie = this.getCookie("user_id");
        this.id = !userIdFromCookie ? this.genUuid() : userIdFromCookie;
        document.cookie = "user_id=" + this.id;
        console.log(this.id);
        return this;
    }

    get() {
        return this.id;
    }
}