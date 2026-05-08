import uuid
from locust import HttpUser, task, between

class CircleGuardUser(HttpUser):
    wait_time = between(1, 3)

    def on_start(self):
        """
        Caso de Uso 1: Login de usuario recurrente.
        """
        response = self.client.post("/api/v1/auth/login", json={
            "username": "super_admin",
            "password": "password"
        })
        
        if response.status_code == 200:
            data = response.json()
            self.token = data.get("token")
            self.anonymous_id = data.get("anonymousId", str(uuid.uuid4()))
        else:
            self.token = None
            self.anonymous_id = str(uuid.uuid4())

    @task
    def submit_health_survey(self):
        """
        Caso de Uso 2: Envío masivo de reportes de salud.
        Estresa el Gateway, el Form Service y el bus de eventos Kafka.
        """
        if not self.token:
            return
            
        headers = {
            "Authorization": f"Bearer {self.token}",
            "X-Anonymous-Id": self.anonymous_id
        }
        
        payload = {
            "anonymousId": self.anonymous_id,
            "symptoms": ["FEVER" if uuid.uuid4().hex[-1] > 'a' else "NONE"],
            "temperature": 36.5,
            "contactWithInfected": False
        }
        
        self.client.post(
            "/api/v1/surveys", 
            json=payload, 
            headers=headers,
            name="POST /api/v1/surveys"
        )