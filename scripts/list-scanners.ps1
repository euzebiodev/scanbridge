$ErrorActionPreference = "Stop"

$deviceManager = New-Object -ComObject WIA.DeviceManager

foreach ($info in $deviceManager.DeviceInfos) {
    if ($info.Type -eq 1) {
        foreach ($property in $info.Properties) {
            if ($property.Name -eq "Name") {
                Write-Output $property.Value
            }
        }
    }
}
