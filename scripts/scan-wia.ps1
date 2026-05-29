param(
    [Parameter(Mandatory = $true)]
    [string]$OutputPath,

    [string]$DeviceName = "",

    [ValidateRange(75, 600)]
    [int]$Dpi = 300,

    [ValidateSet("color", "grayscale", "blackwhite")]
    [string]$ColorMode = "color"
)

$ErrorActionPreference = "Stop"

function Set-WiaProperty {
    param(
        [object]$Properties,
        [string]$Name,
        [object]$Value
    )

    foreach ($property in $Properties) {
        if ($property.Name -eq $Name) {
            $property.Value = $Value
            return
        }
    }
}

$formatJpeg = "{B96B3CAE-0728-11D3-9D7B-0000F81EF32E}"
$deviceManager = New-Object -ComObject WIA.DeviceManager
$deviceInfo = $null

foreach ($info in $deviceManager.DeviceInfos) {
    if ($info.Type -eq 1) {
        $name = ""
        foreach ($property in $info.Properties) {
            if ($property.Name -eq "Name") {
                $name = [string]$property.Value
            }
        }

        if ([string]::IsNullOrWhiteSpace($DeviceName) -or $name -like "*$DeviceName*") {
            $deviceInfo = $info
            break
        }
    }
}

if ($null -eq $deviceInfo) {
    throw "Nenhum scanner WIA encontrado. Confira se o driver do scanner esta instalado no servidor."
}

$device = $deviceInfo.Connect()
$item = $device.Items.Item(1)

$intent = switch ($ColorMode) {
    "color" { 1 }
    "grayscale" { 2 }
    "blackwhite" { 4 }
}

Set-WiaProperty $item.Properties "Horizontal Resolution" $Dpi
Set-WiaProperty $item.Properties "Vertical Resolution" $Dpi
Set-WiaProperty $item.Properties "Current Intent" $intent

$image = $item.Transfer($formatJpeg)
$directory = Split-Path -Parent $OutputPath
if (-not [string]::IsNullOrWhiteSpace($directory)) {
    New-Item -ItemType Directory -Force -Path $directory | Out-Null
}

if (Test-Path -LiteralPath $OutputPath) {
    Remove-Item -LiteralPath $OutputPath -Force
}

$image.SaveFile($OutputPath)
Write-Output $OutputPath
