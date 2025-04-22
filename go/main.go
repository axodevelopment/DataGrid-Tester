package main

import (
	"bytes"
	"crypto/tls"
	"fmt"
	"io"
	"log"
	"net/http"
	"os"
	"os/signal"
	"strconv"
	"syscall"
	"time"
)

func main() {
	// Config
	cacheName := "testCache"
	useAdminCred := false

	username := ""
	password := ""

	serverURL := "https://localhost:11222/rest/v2/caches/" + cacheName

	if useAdminCred {
		username = "admin"
		password = "password"
	} else {
		username = "developer"
		password = "cRspNgHtjMHigzid"
	}

	// HTTP client that skips SSL verification
	tr := &http.Transport{
		TLSClientConfig: &tls.Config{InsecureSkipVerify: true},
	}
	client := &http.Client{Transport: tr}

	// Handle Ctrl+C
	stop := make(chan os.Signal, 1)
	signal.Notify(stop, os.Interrupt, syscall.SIGTERM)

	ticker := time.NewTicker(1 * time.Second)
	defer ticker.Stop()

	fmt.Println("🚀 Starting to push and read data every second. Press Ctrl+C to stop.")

	counter := 0

loop:
	for {
		select {
		case <-stop:
			fmt.Println("\n🛑 Received interrupt signal. Exiting...")
			break loop
		case t := <-ticker.C:
			// Key and value
			key := "key-" + strconv.Itoa(counter)
			value := "time-" + t.Format(time.RFC3339)

			// --- PUT ---
			putReq, err := http.NewRequest("PUT", serverURL+"/"+key, bytes.NewBuffer([]byte(value)))
			if err != nil {
				log.Printf("⚠️  Failed to create PUT request: %v", err)
				continue
			}
			putReq.SetBasicAuth(username, password)
			putReq.Header.Set("Content-Type", "text/plain")

			putResp, err := client.Do(putReq)
			if err != nil {
				log.Printf("⚠️  Failed to PUT: %v", err)
				continue
			}
			putResp.Body.Close()

			if putResp.StatusCode != 204 {
				body, _ := io.ReadAll(putResp.Body)
				log.Printf("⚠️  Unexpected PUT response: %s - %s", putResp.Status, string(body))
				continue
			}

			fmt.Printf("✅ PUT key=%s value=%s\n", key, value)

			// --- GET ---
			getReq, err := http.NewRequest("GET", serverURL+"/"+key, nil)
			if err != nil {
				log.Printf("⚠️  Failed to create GET request: %v", err)
				continue
			}
			getReq.SetBasicAuth(username, password)

			getResp, err := client.Do(getReq)
			if err != nil {
				log.Printf("⚠️  Failed to GET: %v", err)
				continue
			}
			defer getResp.Body.Close()

			body, err := io.ReadAll(getResp.Body)
			if err != nil {
				log.Printf("⚠️  Failed to read GET response: %v", err)
				continue
			}

			fmt.Printf("🔎 Retrieved key=%s value=%s\n", key, string(body))

			counter++
		}
	}

	fmt.Println("Application Exiting...")
}
