</main>
<script>
  (function () {
    // hx-boost は body の innerHTML を差し替えるだけで body 自体は再利用されるため、
    // この script はナビゲーションの度に再実行される。リスナーの多重登録を防ぐため
    // 初回のみ document.body に束縛し、要素参照はイベント発火のたびに取り直す
    if (window.__progressBarBound) return;
    window.__progressBarBound = true;
    var hideTimer = null;
    document.body.addEventListener("htmx:beforeRequest", function () {
      var bar = document.getElementById("progress-bar");
      if (!bar) return;
      clearTimeout(hideTimer);
      bar.style.transition = "none";
      bar.style.width = "0";
      bar.offsetHeight;
      bar.style.transition = "";
      bar.classList.add("active");
      bar.style.width = "80%";
    });
    document.body.addEventListener("htmx:afterRequest", function () {
      var bar = document.getElementById("progress-bar");
      if (!bar) return;
      bar.style.width = "100%";
      hideTimer = setTimeout(function () {
        bar.classList.remove("active");
        bar.style.width = "0";
      }, 250);
    });
  })();
</script>
</body>
</html>
