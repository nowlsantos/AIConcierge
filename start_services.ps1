if (!(Get-Process ollama -ErrorAction SilentlyContinue)) { Start-Process "ollama" -ArgumentList "serve" -WindowStyle Hidden; Start-Sleep -Seconds 2 }; ./gradlew :backend:run
