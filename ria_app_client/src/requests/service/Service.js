import axios from "axios";

class Service {
  getRestClient() {
    if (!this.serviceInstance) {
      this.serviceInstance = axios.create({
        baseURL: '/api/files/download/',
        timeout: 10000,
        headers: {
            'Content-Type': 'application/json'
          },
      });
    }
    return this.serviceInstance;
  }
}

export default (new Service());