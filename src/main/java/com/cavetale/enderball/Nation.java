package com.cavetale.enderball;

import com.cavetale.core.font.DefaultFont;
import com.cavetale.enderball.util.Items;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.block.banner.Pattern;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;

@SuppressWarnings("LineLength")
public enum Nation {
    DENMARK("Denmark", "H4sIAAAAAAAAAC3KsQ6CQBBF0QdoWNbEysqSCuMfWKJ2xlgQWzPCSjbirJkdC/7eEL3dSa4FMiwOpHR1En1gwK4NUt9h9fLsWqGH7sR1tzsxO7HIlHqLvPPxPdBYYHYK4gyABKastue62ZSwWNZDaJ9HVq9jQ30BcyFVJxzt9BrkfyONbYb5PgxB8CuZ+GFN8AUlcpDTnwAAAA==",
            DefaultFont.DENMARK),
    UNITED_KINGDOM("UK", "H4sIAAAAAAAAAJ3OvQrCQBAE4DGHGE9IZ+OjWPrTW4itbC5nOLzswd6m8O01YGEKU2S74WOGtYDB5kRKNy85JAbsrkQRGmy7wN4JPXRfx97fa2L2YmGUWovqEJN7nlmDvq7UrlFeSNULZ4vPRonVN8M0MRssjykmwXAjkwmb1yuc/FA1ponWf+pmUXajN4DFEHvWAm+OsPJfdgEAAA==",
                   DefaultFont.BRITAIN),
    GERMANY("Germany", "H4sIAAAAAAAAAE3Juw7CMAxG4b8JiBAkWJh4FEYuOwNiRaY1VdTgSI4ZeHuKxNDhDEdfBDxWJzK6sdZUBIi7AJc6bF9JuFV62l65uz9IhDXCG/UR60Mu7XAWS/a5Ur9EuJAZq9QIwAUs/g+Xq8f8WHLRUTaYkk5pNtb89i3W4AtmrsszmQAAAA==",
            DefaultFont.GERMANY),
    AUSTRIA("Austria", "H4sIAAAAAAAAAFXJsQrCMBAG4N8EMUbo6OCjOKrdO4irnDXWo/EClxPx7VVw0PHji4DHYkdGh6SViwBxFeD4jOWNJfVKF1s/rmzpeCKRpBHeaIhoNrn0YyvG9tzTMEfoyCyp1AjABcy+hsvVY7otueh7GvyW/hcw+fAu5vACZm89qJsAAAA=",
            DefaultFont.AUSTRIA),
    BRAZIL("Brazil", "H4sIAAAAAAAAAFXJMQvCMBCG4c9EMU3B0cGf4qh2dxBXOeMZgu0Frufgv7eCgw7v8PJEwKM9kNGZdSxVgLgJcOWG9VCEk9LdtlmZ5XIlEdYIb5QjVru+pkcnVux1otwgHMmMVcYIwAUsvw83qMdiX/uqk8zxR+mH2qnZZ59iDm8X07OImwAAAA==",
           null),
    BELGIUM("Belgium", "H4sIAAAAAAAAAE3JuwoCMRBG4d8EMUZYS8FHsfTSW4itzK7jEowzkIzIvr0rWGxxisMXAY/VkYyuXGpSAeI2wKU7Nq8k3BV62G7gnPVza0mES4Q36iOafdbueRJLNlyoXyKcyYyL1AjABSz+D2fVY37QrGWUNabUTqkZm/32LebwBRvSzgycAAAA",
            DefaultFont.BELGIUM),
    CANADA("Canada", "H4sIAAAAAAAAAHXNPQvCMBSF4WOCtEbo6OBPcfRjdxBXuY2xDTY3kFwV/70KDu2Q8fBweA2gsdyT0Nml7CMDZl1D+StWwbOziW6yefVe3KUlZpcMtFBn0GyHaO8HFi/vE3UL1EcScYmzAVDVqP4byiaN+S4OMX2lwZhCLpG2bRwZJrdnX6RgizEpxlQ7JWD2mw8WhQ8OR93KIwEAAA==",
           DefaultFont.CANADA),
    FRANCE("France", "H4sIAAAAAAAAAE3JMQvCMBCG4c8EMU2ho4M/xVHt7iCucq1nDdYLXE7Ef28Fhw7v8PJEwKM+kNGZtaQsQNwEuHTF+pmEe6Wbbd/3ZHzpSIQ1whsNEc1uzP2jFUv2OdFQIRzJjFVKBOACVv+Hs+Kx3Ocx6yQ15tTNqZla/PYl5vAFRMyVH5sAAAA=",
           DefaultFont.FRANCE),
    IRELAND("Ireland", "H4sIAAAAAAAAAE3JsQrCMBAG4N+EYhrB0cFHcdS6O4irXOtZj9YLJCfi21vEoePHFwGPVUNGF85FkgJxG+Dkhs1TlLtMd9u9H2J8bUmVc4Q36iPW+zF1w1FN7HOmvkY4kRlnLRGAC1j+DdcWj+qQxpSnWWBeNq/q1xNfag5f4NQhi5sAAAA=",
            DefaultFont.IRELAND),
    MEXICO("Mexico", "H4sIAAAAAAAAAFXMuwoCMRSE4TFBjFG3tPBRLL30FmIrZ2PcDe6eQHJEfHsjWMRiip8PxgIaiwMJXXzKITJgNwYq3LAeA3uX6C7bVx/EX1ti9slCC3UWzW6I7nFkCfI+UzeHOZGIT5wtyqnB7NdQkjWm+zjEVKRBTW1Nqz8aXUXLssk3nywKH4a3gJm2AAAA",
           DefaultFont.MEXICO),
    NETHERLANDS("Netherlands", "H4sIAAAAAAAAAI3JvQ5BQRAG0M8useYmt1R4FKWfXiFaGde6NtZsMjsi3h6JQiXKk0OAR7Ni413UmooANAtw6YjpNUnslE82v5+Txf2BRaISvHFPaBe5dJe1WLLHlvsJwobNokolAMOA8cdwWj1Gy5KLvqbFn5W/q/lVwODNm5jDE+IRw2vRAAAA",
                DefaultFont.NETHERLANDS),
    POLAND("Poland", "H4sIAAAAAAAAAE3JuwrCQBBG4d9dxHWElBY+iqWX3kJsZYxrdkmchckY8e01YJHTHT4CPFYHNr5E7XMRgDYBLt+xfmaJtfLDtu+ULV5vLBKV4I0bQrXrSt0exbJ9ztwsEU5sFlV6AuACFv+HH5J6zPelK/qjChNzQ5rQ2Gzcl5jDFxCI6COcAAAA",
           DefaultFont.POLAND),
    //RUSSIA("Russia", "H4sIAAAAAAAAAKXJwQ4BMRAG4F8bVDfZo4NHcWT37iCuMqrWRE2TdkS8PRIHZ45fPg9YNB0p7WKpnAXwCwfDR8yvLDEUOunyfmaN+wOJxOJhlQaPdpVyuPSirI8tDTO4DanGItUDmDhMP4ZJ1WK8zimX17T4v8J3NT8WMHrzJmrwBIAiSiUHAQAA",
    //null),
    SPAIN("Spain", "H4sIAAAAAAAAAHXJvQrCMBQG0M/GYo3SVfBRHP3ZHcRVbmtqo+m9kF6Rvr0KDungeDgWMFjsSensYu+FAbsukPkrVp1nV0dqdDO4EOR1qYjZRQujdLMot0Hqx4HV63Ci2xzFkVRd5N4CyAvMfobp5G6Q7yRI/NQS6TVB/l7bVslN08tCn1Q5qjguYPLlkzXDG+0i9RvwAAAA",
          DefaultFont.SPAIN),
    PORTUGAL("Portugal", "H4sIAAAAAAAAAH3MsQrCMBSF4dNGMUbUxclHcWx1dxBXubZpCcYbuLkOvr0RHDo5nOHng+MAg9WRlK5eckgMuL1FHXrsnoF9JzToQXx/uxOzFwejNDpsmpi6x4k16PtC4xL2TKpeODuUS4vFr1FrNpi3KSYpssaEzBDTxLZ/bFZWffPFWuED2z7D7bYAAAA=",
             null),
    SWEDEN("Sweden", "H4sIAAAAAAAAAJ3MsQrCMBSF4WNiMabgKvgojtruDuIqtzG2wXgDyRXp21vBwVEczvDzwbGARt2Q0MnnEhIDdmOgwgXre2DvMl1lO/oY0/PcEbPPFlqot1jtYnK3liXIeKR+CXMgEZ+5WACVweLTUFI0qn2KKU9S4zfSw9D9Y8p9X86nzd75YFF4AbvWklrvAAAA",
           DefaultFont.SWEDEN),
    UKRAINE("Ukraine", "H4sIAAAAAAAAAH3JPQoCMRAG0M9EMUbwAHoTS396C7FdZtcxOxgnkJ0VvL0K1paPFwGP5YGMLlwHKQrEdYCTKzYPUe4q3WybJfXWtHnkpiVVrhHeKEWsdrl096Oa2OtMaYFwIjOuOkQALmD+M9yz95jtSy71M1P8KWDy5ajm8AZ1xLEMoAAAAA==",
            DefaultFont.UKRAINE),
    USA("USA", "H4sIAAAAAAAAAE2JvQrCQBAGP+8Qzw1YWfkolv70KcRW1uQMh5c92NsUvr0GLDIwxTAEeDQXNr5HrakIQIcAl3rsxySxU37ZUWP/eLJIVII3Hgi7Uy7d+yqW7HPjYYvQsllUqQTABWz+DVerx/pcclHMLJflxWp+ruacxBy+DPYRHpkAAAA=",
        DefaultFont.USA),
    ITALY("Italy", "H4sIAAAAAAAAAI3JPQ8BQRAG4NcusUZcqfBTlD56hWhl7qy7jTObzI6If49EoRLlk4cAj+mGjQ9RS8oC0CLApRPm1ySxUT7b8t4li8eaRaISvHFLqFZ9bi5bsWSPPbcThB2bRZVCAIYB44/hrHiM1rnP+poZ/qz6u6pfBQzevIk5PAHLPNgt0QAAAA==",
          DefaultFont.ITALY),
    SWITZERLAND("Switzerland", "H4sIAAAAAAAAAH3JsQrCMBRG4d+EYozQyclHcbS6O4ir3KaxBNsbuLkOvr0UHNpBz3b4PGCxPZHSLUpJmQG/dzCpw25MHIPQQw8Su3tLzFE8rFLvUR+HHJ5n1qTvK/UbuAupRuHiAVQO6+/DhGJRNXnIgqk5jb9J51QvqP1DeUHAatoXq8EH9v2cIuoAAAA=",
                DefaultFont.SWITZERLAND),
    SOUTH_KOREA("South Korea", "H4sIAAAAAAAAAHXMvQrCMBSG4c8EaU1BFycvxdGf3UFc5aSNJbSeQHI6ePcqODRCxpcHXgNoNCcSurmYfGDA7Goo32H79OzaSA/ZR9fdLTG7aKCFeoP1YQztcGbx8rpSv0J9IREXORkAVY3q19Dj1Gksj2EM8UMNZqYkzQgZ2SLp1oai2Wy5yUzKpv6WwOKbE4vCG88mtgYkAQAA",
                null),
    NORWAY("Norway", "H4sIAAAAAAAAAHXJuwoCMRBG4d9EMWZhSwsfxdJLbyG2Msa4hs1OIBkR394VLGJhcYrDZwGNZkdCJ59LSAzYlYEKVyyHwN5lusn6eQ/izxdi9tlCC3UW7SYm1+9ZgryO1C1gDiTiMxcLYGow/z5ULhqzbYopj9KipvifhprwQ8VV1IxNPvtgUXgD30GWRdEAAAA=",
           DefaultFont.NORWAY),
    SCOTLAND("Scotland", "H4sIAAAAAAAAAK3JOw7CMBAFwJdYgHEkDgA3oeTTUyDaaBOMsWLW0mZdcHtAooCCjnI0DjBodqR08jLGzIBbWtTxjNUtsu+FLrpOMVy17VLxbUfMXhyMUnBYbFLuhz1r1PuRwhz2QKpeeHQAphazt2GCkMFkm1OWZzX4vvLr6l4+Cv8voHqxsFZ4AKDDdgIOAQAA",
             null),
    ENGLAND("England", "H4sIAAAAAAAA/y3JuwrCQBAF0Jss4jpCSgs/xdJHbyG2Mq5jHBJnYXdE/HsNWB4OAQHLPTufpVTNBtA6otUbVk81SYXvvnk/1OVyZTMphODcE7rtmNNwMFf/nLhfIB7ZXYpVAtBEzP9GW1PAbJfHXH7TTTvxZd7gC2Y4KAaAAAAA",
            DefaultFont.ENGLAND),
    WALES("Wales", "H4sIAAAAAAAA/3WMPQ+CMBRFLzQq1sjo4E9h9GN3MK7mgQWq5TUpT4z/3pA4lIG7nZycqwGFzYmEbib01jOg9xlS+8Cus2yqQLUUn9aKuZfEbIKGEmo08oPz1evMYuV7pWaN7EIiJnCvASwzrP4MNbRBYXH0zgeMi13tfOTy2KWln8vSoY3UdvLY+efso+snGZCM+GZJ8AMX5/pPCgEAAA==",
          null),
    CZECH_REPUBLIC("Czech Republic", "H4sIAAAAAAAA/1XMvQrCQBAE4EkO8bxASgsfxdKf3kJsZY1n7kjcg80a8e1NwOIcmGL4YBxgUB1I6eJliIkBt7Eo4x3rZ2TfCD10+w5R/fVGzF4cjFLrUO/61HRH1qifM7Ur2BOpeuHBYTq1WP42zBjEYLFPfRLMyawcQ0b1H6lmVE0t5vliLfAFO0oP3rcAAAA=",
                   null),
    SLOVAKIA("Slovakia", "H4sIAAAAAAAA/3XNPwvCMBQE8GuDGlPo5uJHcfTP7iCu8hrTEkwTeHkd/PYqOKRDx+PH3RlAoTmT0N1x9ikCZq9R+yd2o4/OMvVy6MLkHh3F6NhACQ0G7TEk+7pE8fK+0bCFvpKI45gNgLXG5p9Rc1ZYnVJI/JUWJYWSMKORC2pKUn1Ii4s2L9Xq0c7OgOoXpygVPr8i1Z8HAQAA",
             null),
    CROATIA("Croatia", "H4sIAAAAAAAA/43MPQvCMBSF4dMGNabQ0cGf4ujH7iCuco1pDaY3kFwR/70VHOIgOJzh5YFjAIVmS0JHl7KPDJilRu0vWAyenU3Uyepx9eJOZ2J2yUAJ9QbtOkR727F4eR6on0PvScQlzgbAVGP2adQpK0w2McQ0SouSBlsQSlJdiL+sDuVj8+fjN7XjqnfeWSq8AMuLX4YIAQAA",
            null),
    FINLAND("Finland", "H4sIAAAAAAAA/3WLPQvCMBRFbxuKMQU3F3+Kox+7g7jKS4xtaPoCyevgv9eCgw69cIfD4RhAoT2R0M3nEhIDZqdRhwe2Y2DvMj1lb+Pk75aYfTZQQp3B5hCTG84sQV5X6tbQFxLxmYsB0GisvoxaokJzTDFlzPtTeVGNZUmpvreLmfvN2s+rGSeWCm/XseAq7AAAAA==",
            null),
    HUNGARY("Hungary", "H4sIAAAAAAAA/1XMuwoCMRCF4bMbxDXilhY+iqWX3kJsZYxxDRsnMBkR394IFrE4xc8HxwIG8x0pnbzkkBiwqw5tuGL5COyd0E3Xr3tQf74QsxcLozRY9JuY3LhnDfo+0jBDdyBVL5wtymmH6a/Rxmww2aaYpEiPmlxN+COpaVHWfPPJ2uADYLHgq7YAAAA=",
            null),
    GREECE("Greece", "H4sIAAAAAAAA/1XJPQ8BQRAG4Pd2I9ZeolT4KUofvUK0Ms44E2c2mR0R/x6JgvLJk4GIdk1Oe7YqRYE8Twhywuwmyp3R2RePizgfjqTKlhGd+ozpcijddaMu/txRP0Hakjub1gwgJIy/Rqg1YrQqQ7H3tPgtt78Cmg/v6g1e7f1TsZsAAAA=",
           null),
    TURKEY("Turkey", "H4sIAAAAAAAA/3XLPQvCMBSF4dMGNUbo5ORPcfRjdxBXuUljDTY3kFwF/70UHNqhZ3t5OAZQ2JxI6OZzCYkBs9OoQ4ttDOxdpofss2/vlph9NlBCnUFz6JN7nVmCfK/UraEvJOIzFwNgqbH6N+rPU2FxTH3KGDamWOZIOZtG1kxuUmbJzlN0EwKqId8sFX5zQGzbBgEAAA==",
           null),
    ICELAND("Iceland", "H4sIAAAAAAAA/53JsQrCMBRG4b8Nahqhm4uP4qh2dxBXuY2xBNMbuLkdfHspODiJeLbD5wCD9ZGULkFKzAy4rUUdb9iMkYMXuuuuT1O49sQcxMEoDQ7tPmX/6FijPs80NLAnUg3CxQFYWqzej9oXg8UhpyyY+5HGv6j4D2q/EVDNO7FWeAHbx7xKBgEAAA==",
            null),
    ALBANIA("Albania", "H4sIAAAAAAAA/3WMPQvCMBRFbxvEGKEuTv4URz92B3GV1zQtoekLvD4H/70RHLo43OFwONcBBtsLKT2CzDEz4A4WdeywnyIHL9TrUUL3bIk5iINRGhyaU8p+vLJGfd9p2MDeSDUIzw7l0mL9Y5g+ZYPVOacsRe2wcPXk/6p2WTVl1RdfrBU+3RjlH7UAAAA=",
            null),
    LATVIA("Latvia", "H4sIAAAAAAAA/33LPQvCMBSF4dNGMUaom4M/paMfu4O4ym2MITTeQHI7+O+14NAunu3l4RhAYXMioZvLJSQGzF6jDg/sXoGdzfSUtotk+3tHzC4bKCFv0Bxisv2ZJcj7Sn4NfSERl7kYAAuN1a+hfB4UlscUU/5Sg7nRxLZ/bParbZnQuGrMgaXCB0r4++DUAAAA",
           null),
    ROMANIA("Romania", "H4sIAAAAAAAA/1XMuwoCMRCF4bMbxZiFLS18FEsvvYXYyhjjGowTmIyIb+8KFrE4xc8HxwEG3ZaUjkFKzAy4pUUbL1g8IgcvdNXV6xY1nM7EHMTBKA0O/Tplf9+xRn0faJjD7kk1CBeH8dRi9mu0qRhMNzllGaVDTb6myR9JTf245ptP1gYfViSH67YAAAA=",
            null),
    BULGARIA("Bulgaria", "H4sIAAAAAAAA/03JuwoCMRBG4X8TFmOELS18FEsvvYXYyhjHNbhOYDIivr0rWGxxisMXAY/FjoxOrDUXAeIqwOUrls8snJRutn7fs/H5QiKsEd6oj+g2Q0mPvVi2z5H6OcKBzFilRgAuYPZ/uFQ92m0Zio7SYko6pW6s+e1LrMEXuQZ0RJsAAAA=",
             null),
    ARGENTINA("Argentina", "H4sIAAAAAAAA/43JsQrCMBRG4b9NxRjFF/BNHLXdHcS13MY0BuMNJLeDr+ETS8HBSTzb4TOAwroloYvLJSQGNi+NOlyxewR2NtMo+xj8TfohTq4fiNllAyXkDbaHmOy9YwnyPJNfQZ9IxGUuBkCjsfw8alsUFscUU8bcf6TGmL6s+WlANe/EUuENbFXwAdgAAAA=",
              DefaultFont.ARGENTINA),
    JAPAN("Japan", "H4sIAAAAAAAA/43LvQrCMBTF8dNGMUbp6MM4+rE7iKvcxthc2t5AekV8DZ9YCg5182x/fhwHGKwOpHQJeeAkwPptUfINm54l+Ex33T4ja7jWJBKyg1FqHKpdl3x7FGV9nalZwp5INWQZHICZxeLbMDHWBvN96lLGuImVvZ9Q9UMx/vsCijEfogU+I9VCK9IAAAA=", DefaultFont.JAPAN),
    ;

    public final String name;
    public final String base64;
    public final ItemStack bannerItem;
    public final DyeColor bannerDyeColor;
    public final List<Pattern> bannerPatterns;
    public final DefaultFont defaultFont;
    public final Component component;

    Nation(final String name, final String base64, final DefaultFont defaultFont) {
        this.name = name;
        this.base64 = base64;
        // Icon
        this.defaultFont = defaultFont;
        this.component = defaultFont != null
            ? defaultFont.component
            : Component.empty();
        // We copy only the material and patterns from the existing item.
        ItemStack oldItem = Items.deserialize(base64);
        BannerMeta oldMeta = (BannerMeta) oldItem.getItemMeta();
        BannerMeta meta = (BannerMeta) Bukkit.getItemFactory().getItemMeta(oldItem.getType());
        this.bannerDyeColor = Items.getDyeColor(oldItem.getType());
        this.bannerPatterns = oldMeta.getPatterns();
        meta.setPatterns(bannerPatterns);
        meta.addItemFlags(ItemFlag.values());
        meta.displayName(Component.text().append(component)
                         .append(Component.text(name, TextColor.color(0xFFFFFF)))
                         .decoration(TextDecoration.ITALIC, false).build());
        this.bannerItem = new ItemStack(oldItem.getType());
        this.bannerItem.setItemMeta(meta);
    }
}
