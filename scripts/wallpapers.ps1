<#
.SYNOPSIS
    Turns the painted watch mock-ups in skin-images/ into wallpaper resources.

.DESCRIPTION
    Every source picture is round art inside a painted bezel ring and a margin,
    so it cannot be used as it is. This finds the art circle, crops to it, scales
    to the watch's 480x480 and writes one JPEG per slot.

    The file name is the assignment: skin-images/<theme>/<slot>.png becomes
    wp_<theme>_<slot>.jpg. Swapping two pictures means swapping two file names.

    -ReviewDir also writes one sheet per theme with every slot dimmed and sample
    text drawn on, to judge an assignment before any code uses it. The dimming
    there is a preview; the app's ScrimSolver and its test are authoritative.

    System.Drawing rather than Python or ImageMagick: neither is installed on
    this machine, and System.Drawing ships with Windows.

.EXAMPLE
    powershell -File scripts/wallpapers.ps1

.EXAMPLE
    powershell -File scripts/wallpapers.ps1 -Source staged -OutDir out -ReviewDir review
#>
param(
    [string]$Source = (Join-Path $PSScriptRoot '..\skin-images'),
    [string]$OutDir = (Join-Path $PSScriptRoot '..\adapters\driving\ui-compose\src\main\res\drawable-nodpi'),
    [string]$ReviewDir,
    [string[]]$Theme = @('dragonball', 'sailor-moon', 'spy-family'),
    [int]$Quality = 92
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

$Size = 480

# Theme folder -> resource infix. Resource names allow only [a-z0-9_].
$ThemeKeys = @{ 'dragonball' = 'dragon_ball'; 'sailor-moon' = 'sailor_moon'; 'spy-family' = 'spy_family' }

# Every surface that can carry a wallpaper, in screen order. The last three may
# be missing: the app then shows their parent's picture, and so does the sheet.
$Slots = [ordered]@{
    'chrono'         = $null
    'rest'           = $null
    'rest-running'   = $null
    'rest-over'      = 'rest-running'
    'counter'        = $null
    'workouts'       = $null
    'settings'       = $null
    'rest-editor'    = 'rest'
    'workout-picker' = 'workouts'
}
$ListSlots = @('settings', 'rest-editor', 'workout-picker')

$Lin = [double[]]::new(256)
for ($i = 0; $i -lt 256; $i++) {
    $c = $i / 255
    $Lin[$i] = if ($c -le 0.04045) { $c / 12.92 } else { [math]::Pow(($c + 0.055) / 1.055, 2.4) }
}
function Get-Luminance([System.Drawing.Color]$p) { 0.2126 * $Lin[$p.R] + 0.7152 * $Lin[$p.G] + 0.0722 * $Lin[$p.B] }

# Kasa fit: x^2 + y^2 + Dx + Ey + F = 0 by least squares, solved with Cramer's rule.
function Get-FittedCircle($pts) {
    $sxx = 0.0; $sxy = 0.0; $syy = 0.0; $sx = 0.0; $sy = 0.0; $sxz = 0.0; $syz = 0.0; $sz = 0.0
    foreach ($p in $pts) {
        $x = $p[0]; $y = $p[1]; $z = - ($x * $x + $y * $y)
        $sxx += $x * $x; $sxy += $x * $y; $syy += $y * $y; $sx += $x; $sy += $y
        $sxz += $x * $z; $syz += $y * $z; $sz += $z
    }
    $n = [double]$pts.Count
    $det = { param($a, $b, $c, $d, $e, $f, $g, $h, $i) $a * ($e * $i - $f * $h) - $b * ($d * $i - $f * $g) + $c * ($d * $h - $e * $g) }
    $m = & $det $sxx $sxy $sx $sxy $syy $sy $sx $sy $n
    $dD = & $det $sxz $sxy $sx $syz $syy $sy $sz $sy $n
    $dE = & $det $sxx $sxz $sx $sxy $syz $sy $sx $sz $n
    $dF = & $det $sxx $sxy $sxz $sxy $syy $syz $sx $sy $sz
    $cx = - ($dD / $m) / 2; $cy = - ($dE / $m) / 2
    @($cx, $cy, [math]::Sqrt($cx * $cx + $cy * $cy - $dF / $m))
}

<#
    Walks 48 rays inward from the edge of the square to the first stretch of
    real art, then fits a circle through those points.

    A light margin is followed by a near-black ring, and the art only starts
    after the ring: the painted drop shadow around it is neither margin nor ring,
    so it is not trusted as art until the ring has been crossed. A black margin
    (Spy x Family) has no ring; the art starts at the first stretch that is not
    black. Ten pixels of art are required, so a glossy highlight in the ring is
    not mistaken for the picture.

    Dark art touching the edge (a black cat, black hair) makes its rays overshoot
    inward. Those points sit inside the true circle, so they are dropped and the
    circle is fitted again.
#>
function Find-ArtCircle([System.Drawing.Bitmap]$bmp) {
    $w = $bmp.Width; $h = $bmp.Height
    $corner = $bmp.GetPixel(4, 4)
    $lightMargin = (Get-Luminance $corner) -gt 0.2
    $cx0 = $w / 2; $cy0 = $h / 2; $maxR = [math]::Min($w, $h) / 2 - 2
    $points = New-Object System.Collections.Generic.List[double[]]

    for ($k = 0; $k -lt 48; $k++) {
        $angle = 2 * [math]::PI * $k / 48
        $dx = [math]::Cos($angle); $dy = [math]::Sin($angle)
        $crossedRing = -not $lightMargin
        $run = 0
        for ($t = [int]$maxR; $t -gt $maxR * 0.5; $t--) {
            $p = $bmp.GetPixel([int]($cx0 + $dx * $t), [int]($cy0 + $dy * $t))
            $dark = (Get-Luminance $p) -lt 0.015
            $margin = ([math]::Abs($p.R - $corner.R) + [math]::Abs($p.G - $corner.G) + [math]::Abs($p.B - $corner.B)) -lt 45
            if ($dark) { $crossedRing = $true; $run = 0 }
            elseif ($margin -or -not $crossedRing) { $run = 0 }
            else {
                $run++
                if ($run -ge 10) { $points.Add(@(($cx0 + $dx * ($t + 9)), ($cy0 + $dy * ($t + 9)))); break }
            }
        }
    }
    if ($points.Count -lt 24) { throw "found the art edge on only $($points.Count) of 48 rays" }

    $circle = Get-FittedCircle $points
    $kept = New-Object System.Collections.Generic.List[double[]]
    foreach ($p in $points) {
        $d = [math]::Sqrt([math]::Pow($p[0] - $circle[0], 2) + [math]::Pow($p[1] - $circle[1], 2))
        if ($d -ge $circle[2] * 0.985) { $kept.Add($p) }
    }
    if ($kept.Count -lt 16) { throw "only $($kept.Count) rays agree on the art edge" }
    $circle = Get-FittedCircle $kept

    if ($circle[2] -lt $w * 0.35 -or [math]::Abs($circle[0] - $cx0) -gt $w * 0.06 -or [math]::Abs($circle[1] - $cy0) -gt $h * 0.06) {
        throw ("implausible art circle: centre {0:N0},{1:N0} radius {2:N0} in a {3}x{4} image" -f $circle[0], $circle[1], $circle[2], $w, $h)
    }
    @($circle[0], $circle[1], $circle[2], $kept.Count)
}

# Crops to the art circle, inset past its anti-aliased edge, and blacks out the
# corners — the round screen hides them, but a square preview must not show bezel.
function New-Wallpaper([string]$path) {
    $src = New-Object System.Drawing.Bitmap $path
    try {
        $circle = Find-ArtCircle $src
        $r = $circle[2] * 0.985
        $bmp = New-Object System.Drawing.Bitmap $Size, $Size
        $g = [System.Drawing.Graphics]::FromImage($bmp)
        $g.InterpolationMode = 'HighQualityBicubic'; $g.SmoothingMode = 'AntiAlias'; $g.PixelOffsetMode = 'HighQuality'
        $from = New-Object System.Drawing.RectangleF ([float]($circle[0] - $r)), ([float]($circle[1] - $r)), ([float](2 * $r)), ([float](2 * $r))
        $g.DrawImage($src, (New-Object System.Drawing.RectangleF 0, 0, $Size, $Size), $from, [System.Drawing.GraphicsUnit]::Pixel)
        $outside = New-Object System.Drawing.Drawing2D.GraphicsPath
        $outside.FillMode = 'Alternate'
        $outside.AddRectangle((New-Object System.Drawing.Rectangle -1, -1, ($Size + 2), ($Size + 2)))
        $outside.AddEllipse(0, 0, $Size, $Size)
        $g.FillPath([System.Drawing.Brushes]::Black, $outside)
        $g.Dispose()
        @{ Bitmap = $bmp; Circle = $circle }
    }
    finally { $src.Dispose() }
}

# Preview of the app's scrim: the lowest black overlay, blended in sRGB space as
# Compose does, at which #CFCFCF text reaches 4.5:1 against the 99th-percentile
# pixel of the text zone. Floor 0.2, as in the app.
function Get-PreviewScrim([System.Drawing.Bitmap]$img, [double]$zone) {
    $argb = New-Object System.Collections.Generic.List[int]
    $c = $Size / 2; $rz = $zone * $Size / 2
    for ($y = 0; $y -lt $Size; $y += 3) {
        for ($x = 0; $x -lt $Size; $x += 3) {
            if ((($x - $c) * ($x - $c) + ($y - $c) * ($y - $c)) -le $rz * $rz) { $argb.Add($img.GetPixel($x, $y).ToArgb()) }
        }
    }
    $target = ($Lin[207] + 0.05) / 4.5 - 0.05
    $lo = 0.0; $hi = 1.0
    for ($it = 0; $it -lt 10; $it++) {
        $a = ($lo + $hi) / 2; $m = 1 - $a
        $lum = [double[]]::new($argb.Count); $j = 0
        foreach ($v in $argb) {
            $lum[$j++] = 0.2126 * $Lin[[int]($m * (($v -shr 16) -band 255))] + 0.7152 * $Lin[[int]($m * (($v -shr 8) -band 255))] + 0.0722 * $Lin[[int]($m * ($v -band 255))]
        }
        [array]::Sort($lum)
        if ($lum[[int](($lum.Count - 1) * 0.99)] -le $target) { $hi = $a } else { $lo = $a }
    }
    [math]::Max(0.2, $hi)
}

function New-ReviewSheet([string]$themeName, $crops) {
    $cell = 330; $watch = 300; $head = 56; $caption = 58
    $sheet = New-Object System.Drawing.Bitmap (3 * $cell), ($head + 3 * ($watch + $caption))
    $g = [System.Drawing.Graphics]::FromImage($sheet)
    $g.SmoothingMode = 'AntiAlias'; $g.TextRenderingHint = 'AntiAlias'; $g.InterpolationMode = 'HighQualityBicubic'
    $g.Clear([System.Drawing.Color]::FromArgb(27, 27, 27))
    $mid = New-Object System.Drawing.StringFormat; $mid.Alignment = 'Center'; $mid.LineAlignment = 'Center'
    $font = { param($px) New-Object System.Drawing.Font 'Segoe UI', ([float]$px), ([System.Drawing.FontStyle]::Regular), ([System.Drawing.GraphicsUnit]::Pixel) }
    $brush = { param($hex) New-Object System.Drawing.SolidBrush ([System.Drawing.ColorTranslator]::FromHtml($hex)) }
    $shadow = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(70, 0, 0, 0))
    $text = {
        param($s, $px, $hex, $x, $y)
        $f = & $font $px
        foreach ($o in @(@(-1.6, 0), @(1.6, 0), @(0, -1.6), @(0, 1.6), @(-1.2, -1.2), @(1.2, 1.2), @(-1.2, 1.2), @(1.2, -1.2))) {
            $g.DrawString($s, $f, $shadow, [float]($x + $o[0]), [float]($y + $o[1]), $mid)
        }
        $g.DrawString($s, $f, (& $brush $hex), [float]$x, [float]$y, $mid)
    }
    $pill = {
        param($x, $y, $d)
        $g.FillEllipse((& $brush '#1E1E1E'), [float]($x - $d / 2), [float]($y - $d / 2), [float]$d, [float]$d)
        $g.DrawEllipse((New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb(120, 255, 255, 255)), 1.2), [float]($x - $d / 2), [float]($y - $d / 2), [float]$d, [float]$d)
    }

    $g.DrawString("$themeName  -  proposed wallpaper per screen (preview colours; real palettes come with the skin)", (& $font 17), (& $brush '#E6E6E6'), 14, 18)

    $k = 0
    foreach ($slot in $Slots.Keys) {
        $source = $slot
        if (-not $crops.Contains($source)) { $source = $Slots[$slot] }
        $img = $crops[$source].Bitmap
        $isList = $ListSlots -contains $slot
        $zone = if ($isList) { 0.9 } else { 0.62 }
        $alpha = Get-PreviewScrim $img $zone

        $ox = ($k % 3) * $cell + ($cell - $watch) / 2
        $oy = $head + [math]::Floor($k / 3) * ($watch + $caption)
        $cx = $ox + $watch / 2; $cy = $oy + $watch / 2; $u = $watch / 480.0

        $round = New-Object System.Drawing.Drawing2D.GraphicsPath
        $round.AddEllipse([float]$ox, [float]$oy, [float]$watch, [float]$watch)
        $g.SetClip($round)
        $g.DrawImage($img, $ox, $oy, $watch, $watch)
        if ($isList) {
            $g.FillPath((New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb([int](255 * $alpha), 0, 0, 0))), $round)
        }
        else {
            $grad = New-Object System.Drawing.Drawing2D.PathGradientBrush $round
            $grad.CenterColor = [System.Drawing.Color]::FromArgb([int](255 * $alpha), 0, 0, 0)
            $grad.SurroundColors = @([System.Drawing.Color]::FromArgb([int](255 * [math]::Min($alpha, 0.3)), 0, 0, 0))
            $blend = New-Object System.Drawing.Drawing2D.Blend 3
            $blend.Factors = [single[]]@(0, 1, 1); $blend.Positions = [single[]]@(0, 0.38, 1)
            $grad.Blend = $blend
            $g.FillPath($grad, $round)
        }

        if ($slot -eq 'rest-running' -or $slot -eq 'rest-over') {
            $inset = 10 * $u; $d = $watch - 2 * $inset
            $g.DrawEllipse((New-Object System.Drawing.Pen ([System.Drawing.Color]::Black), (22 * $u)), [float]($ox + $inset), [float]($oy + $inset), [float]$d, [float]$d)
            $g.DrawEllipse((New-Object System.Drawing.Pen ([System.Drawing.ColorTranslator]::FromHtml('#333333')), (8 * $u)), [float]($ox + $inset), [float]($oy + $inset), [float]$d, [float]$d)
            $sweep = if ($slot -eq 'rest-over') { 360 } else { 250 }
            $g.DrawArc((New-Object System.Drawing.Pen ([System.Drawing.ColorTranslator]::FromHtml('#FFD24D')), (8 * $u)), [float]($ox + $inset), [float]($oy + $inset), [float]$d, [float]$d, -90, $sweep)
        }
        $g.ResetClip()
        $g.DrawEllipse((New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb(60, 60, 60)), 1.5), [float]$ox, [float]$oy, [float]$watch, [float]$watch)

        switch ($slot) {
            'chrono' {
                & $text 'CHRONO' (20 * $u) '#DADADA' $cx ($cy - 78 * $u)
                & $text '12:34' (80 * $u) '#8FE3FF' $cx ($cy - 18 * $u)
                & $text 'lap 2 · 01:05' (22 * $u) '#CFCFCF' $cx ($cy + 40 * $u)
                & $pill ($cx - 100 * $u) ($cy + 110 * $u) (96 * $u); & $pill $cx ($cy + 110 * $u) (80 * $u); & $pill ($cx + 100 * $u) ($cy + 110 * $u) (80 * $u)
            }
            'rest' {
                & $text 'REST' (20 * $u) '#DADADA' $cx ($cy - 70 * $u)
                foreach ($i in -1, 0, 1) { & $pill ($cx + $i * 122 * $u) $cy (104 * $u); & $text @('1:00', '1:30', '2:00')[$i + 1] (28 * $u) '#FFD24D' ($cx + $i * 122 * $u) $cy }
                & $text 'tap to start · hold to edit' (18 * $u) '#CFCFCF' $cx ($cy + 76 * $u)
            }
            'rest-running' {
                & $text 'REST' (20 * $u) '#DADADA' $cx ($cy - 84 * $u)
                & $text '1:12' (76 * $u) '#FFD24D' $cx ($cy - 24 * $u)
                & $text 'buzzes at zero' (20 * $u) '#CFCFCF' $cx ($cy + 30 * $u)
                & $pill ($cx - 58 * $u) ($cy + 104 * $u) (96 * $u); & $pill ($cx + 58 * $u) ($cy + 104 * $u) (96 * $u)
            }
            'rest-over' {
                & $text 'REST OVER' (20 * $u) '#FFD24D' $cx ($cy - 84 * $u)
                & $text '0:00' (76 * $u) '#FFD24D' $cx ($cy - 24 * $u)
                & $pill ($cx - 64 * $u) ($cy + 80 * $u) (96 * $u)
                $g.FillEllipse((& $brush '#FFD24D'), [float]($cx + 8 * $u), [float]($cy + 24 * $u), [float](112 * $u), [float](112 * $u))
            }
            'counter' {
                & $text 'SETS' (20 * $u) '#DADADA' $cx ($cy - 72 * $u)
                & $pill ($cx - 128 * $u) $cy (96 * $u); & $pill ($cx + 128 * $u) $cy (96 * $u)
                & $text '3' (92 * $u) '#F2F2F2' $cx $cy
                & $text 'hold number to reset' (18 * $u) '#CFCFCF' $cx ($cy + 80 * $u)
            }
            'workouts' {
                & $text 'WORKOUTS' (20 * $u) '#DADADA' $cx ($cy - 80 * $u)
                foreach ($i in -1, 0, 1) { & $pill ($cx + $i * 128 * $u) ($cy - 10 * $u) (108 * $u); & $text @('Weights', 'Treadmill', 'Bench')[$i + 1] (20 * $u) '#F2F2F2' ($cx + $i * 128 * $u) ($cy + 64 * $u) }
                & $text 'tap to start · hold to change' (18 * $u) '#CFCFCF' $cx ($cy + 100 * $u)
            }
            'rest-editor' {
                & $text 'REST' (20 * $u) '#DADADA' $cx ($cy - 150 * $u)
                & $text '1  :  30' (52 * $u) '#FFD24D' $cx ($cy - 40 * $u)
                & $text '0      25' (36 * $u) '#DADADA' $cx ($cy - 96 * $u)
                & $text '2      35' (36 * $u) '#DADADA' $cx ($cy + 16 * $u)
                & $text 'min · sec' (18 * $u) '#CFCFCF' $cx ($cy + 58 * $u)
                $g.FillEllipse((& $brush '#9BE58A'), [float]($cx - 44 * $u), [float]($cy + 82 * $u), [float](88 * $u), [float](88 * $u))
            }
            default {
                $title = if ($slot -eq 'settings') { 'SCREENS' } else { 'WORKOUT' }
                $rows = if ($slot -eq 'settings') { @('Chrono', 'Rest', 'Counter', 'Workouts') } else { @('Treadmill', 'Weight machines', 'Bench press', 'Rowing machine') }
                & $text $title (20 * $u) '#DADADA' $cx ($cy - 150 * $u)
                for ($i = 0; $i -lt 4; $i++) {
                    $ry = $cy - 118 * $u + $i * 66 * $u; $rw = 330 * $u; $rh = 52 * $u; $rx = $cx - $rw / 2
                    $row = New-Object System.Drawing.Drawing2D.GraphicsPath
                    $row.AddArc([float]$rx, [float]$ry, [float]$rh, [float]$rh, 90, 180)
                    $row.AddArc([float]($rx + $rw - $rh), [float]$ry, [float]$rh, [float]$rh, 270, 180)
                    $row.CloseFigure()
                    $fill = if ($slot -eq 'workout-picker' -and $i -eq 1) { '#FFD24D' } else { '#1E1E1E' }
                    $g.FillPath((& $brush $fill), $row)
                    $ink = if ($fill -eq '#FFD24D') { '#000000' } else { '#F2F2F2' }
                    $g.DrawString($rows[$i], (& $font (24 * $u)), (& $brush $ink), [float]($rx + 26 * $u), [float]($ry + 10 * $u))
                }
            }
        }

        $label = if ($crops.Contains($slot)) { $slot } else { "$slot  (uses $($Slots[$slot]))" }
        $g.DrawString($label, (& $font 16), (& $brush '#E6E6E6'), [float]($ox + $watch / 2), [float]($oy + $watch + 18), $mid)
        $g.DrawString(("dim {0:N2}" -f $alpha), (& $font 14), (& $brush '#9A9A9A'), [float]($ox + $watch / 2), [float]($oy + $watch + 40), $mid)
        $k++
    }
    $g.Dispose()
    $sheet
}

$jpeg = [System.Drawing.Imaging.ImageCodecInfo]::GetImageEncoders() | Where-Object { $_.MimeType -eq 'image/jpeg' }
$params = New-Object System.Drawing.Imaging.EncoderParameters 1
$params.Param[0] = New-Object System.Drawing.Imaging.EncoderParameter ([System.Drawing.Imaging.Encoder]::Quality), ([long]$Quality)

New-Item -ItemType Directory -Force $OutDir | Out-Null
if ($ReviewDir) { New-Item -ItemType Directory -Force $ReviewDir | Out-Null }

foreach ($themeName in $Theme) {
    $key = $ThemeKeys[$themeName]
    if (-not $key) { throw "no resource name for theme '$themeName'; add it to `$ThemeKeys" }

    $crops = [ordered]@{}
    foreach ($slot in $Slots.Keys) {
        $file = Join-Path (Join-Path $Source $themeName) "$slot.png"
        if (-not (Test-Path $file)) {
            if ($null -eq $Slots[$slot]) { throw "$themeName is missing $slot.png, which has no fallback" }
            continue
        }
        $made = New-Wallpaper $file
        $out = Join-Path $OutDir ("wp_{0}_{1}.jpg" -f $key, $slot.Replace('-', '_'))
        $made.Bitmap.Save($out, $jpeg, $params)
        $crops[$slot] = $made
        "{0,-12} {1,-15} centre {2,4:N0},{3,4:N0}  radius {4,4:N0}  rays {5,2}  {6,3} KB" -f $themeName, $slot, $made.Circle[0], $made.Circle[1], $made.Circle[2], $made.Circle[3], [int]((Get-Item $out).Length / 1024)
    }

    if ($ReviewDir) {
        $sheet = New-ReviewSheet $themeName $crops
        $sheet.Save((Join-Path $ReviewDir "review_$key.png"))
        $sheet.Dispose()
    }
    foreach ($made in $crops.Values) { $made.Bitmap.Dispose() }
}
