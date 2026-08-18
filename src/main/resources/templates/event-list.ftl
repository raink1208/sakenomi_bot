<#include "_header.ftl">
<section>
  <div class="page-head">
    <h1>イベント一覧</h1>
    <a class="button" href="/g/${guildId?c}/events/new">＋ イベント作成</a>
  </div>
  <div class="tabs">
    <a class="tab <#if status == 'recruiting'>active</#if>" href="/g/${guildId?c}/events?status=recruiting">募集中</a>
    <a class="tab <#if status == 'archived'>active</#if>" href="/g/${guildId?c}/events?status=archived">アーカイブ</a>
  </div>

  <#if events?size == 0>
    <p class="empty">イベントがありません。</p>
  <#else>
    <table class="event-table">
      <thead>
        <tr>
          <th>第n回</th>
          <th>名前</th>
          <th>エリア</th>
          <th>開催日</th>
          <th>幹事</th>
          <th>参加人数</th>
        </tr>
      </thead>
      <tbody>
        <#list events as event>
          <tr>
            <td data-label="第n回">第${event.number}回</td>
            <td data-label="名前" class="primary-cell"><a href="/g/${guildId?c}/events/${event.id?c}">${event.name}</a></td>
            <td data-label="エリア">${event.area}</td>
            <td data-label="開催日">${event.date}</td>
            <td data-label="幹事">${event.organizerName}</td>
            <td data-label="参加人数">${event.participantCount!""}</td>
          </tr>
        </#list>
      </tbody>
    </table>
  </#if>
</section>
<#include "_footer.ftl">
